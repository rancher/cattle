package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.LoadBalancerConfigTable.LOAD_BALANCER_CONFIG;
import static io.cattle.platform.core.model.tables.LoadBalancerListenerTable.LOAD_BALANCER_LISTENER;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerServiceCreate extends AbstractObjectProcessHandler {

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    GenericMapDao mapDao;

    @Inject
    LoadBalancerService lbService;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    GenericResourceDao resourceDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { ServiceDiscoveryConstants.PROCESS_SERVICE_CREATE };
    }

    @Override
    @SuppressWarnings("unchecked")
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Service service = (Service) state.getResource();
        if (!service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())) {
            return null;
        }
        String lbName = sdService.getLoadBalancerName(service);
        Map<String, Object> launchConfigData = sdService.buildLaunchData(service);
        // 1. create load balancer config
        Map<String, Object> lbConfigData = (Map<String, Object>) DataAccessor.field(service,
                ServiceDiscoveryConstants.FIELD_LOAD_BALANCER_CONFIG,
                jsonMapper,
                Map.class);
        
        if (lbConfigData == null) {
            lbConfigData = new HashMap<String, Object>();
        }

        LoadBalancerConfig lbConfig = createDefaultLoadBalancerConfig(lbName, lbConfigData,
                service);
        
        // 2. add listeners to the config based on the ports info
        createListeners(service, lbConfig, launchConfigData);

        // 3. create a load balancer
        createLoadBalancer(service, lbName, lbConfig, launchConfigData);
        return null;
    }

    private void createLoadBalancer(Service service, String lbName, LoadBalancerConfig lbConfig,
            Map<String, Object> launchConfigData) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null, LOAD_BALANCER.ACCOUNT_ID, service.getAccountId());
        if (lb == null) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", lbName);
            data.put(LoadBalancerConstants.FIELD_LB_CONFIG_ID, lbConfig.getId());
            data.put(LoadBalancerConstants.FIELD_LB_SERVICE_ID, service.getId());
            data.put(LoadBalancerConstants.FIELD_LB_NETWORK_ID, sdService.getServiceNetworkId(service));
            data.put(
                    LoadBalancerConstants.FIELD_LB_INSTANCE_IMAGE_UUID,
                    launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID));
            data.put(
                    LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE,
                    DataAccessor.fields(service).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE)
                            .withDefault("delegate:///").as(
                                    String.class));
            data.put("accountId", service.getAccountId());

            Map<String, String> labelsStr = sdService.getServiceInstanceLabels(service);
            launchConfigData.put(ServiceDiscoveryConfigItem.LABELS.getRancherName(), labelsStr);

            data.put(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG, launchConfigData);
            lb = objectManager.create(LoadBalancer.class, data);
        }

        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_CREATE, lb, null);
    }

    private LoadBalancerConfig createDefaultLoadBalancerConfig(String defaultName,
            Map<String, Object> lbConfigData, Service service) {
        String name = lbConfigData.get("name") == null ? defaultName : lbConfigData.get("name")
                .toString();
        LoadBalancerConfig lbConfig = objectManager.findOne(LoadBalancerConfig.class,
                LOAD_BALANCER_CONFIG.REMOVED, null,
                LOAD_BALANCER_CONFIG.ACCOUNT_ID, service.getAccountId(),
                LOAD_BALANCER_CONFIG.SERVICE_ID, service.getId());

        if (lbConfig == null) {
            lbConfigData.put("accountId", service.getAccountId());
            lbConfigData.put("name", name);
            lbConfigData.put("serviceId", service.getId());
            lbConfig = objectManager.create(LoadBalancerConfig.class, lbConfigData);
        }
        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_CONFIG_CREATE, lbConfig, null);
        return lbConfig;
    }

    @SuppressWarnings("unchecked")
    private void createListeners(Service service, LoadBalancerConfig lbConfig, Map<String, Object> launchConfigData) {
        // 1. create listeners
        List<String> portDefs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        if (portDefs == null || portDefs.isEmpty()) {
            return;
        }

        Map<Integer, LoadBalancerListener> listeners = new HashMap<>();

        for (String port : portDefs) {
            PortSpec spec = new PortSpec(port);
            if (!port.contains("tcp")) {
                // default to http unless defined otherwise in the compose file
                spec.setProtocol("http");
            }

            Integer publicPort = spec.getPublicPort();
            int privatePort = spec.getPrivatePort();
            if (publicPort == null) {
                publicPort = privatePort;
            }

            if (listeners.containsKey(publicPort)) {
                continue;
            }

            LoadBalancerListener listenerObj = objectManager.findOne(LoadBalancerListener.class,
                    LOAD_BALANCER_LISTENER.SERVICE_ID, service.getId(),
                    LOAD_BALANCER_LISTENER.SOURCE_PORT, publicPort,
                    LOAD_BALANCER_LISTENER.TARGET_PORT, privatePort,
                    LOAD_BALANCER_LISTENER.REMOVED, null,
                    LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId());

            if (listenerObj == null) {
                listenerObj = objectManager.create(LoadBalancerListener.class,
                        LOAD_BALANCER_LISTENER.NAME, sdService.getLoadBalancerName(service) + "_" + publicPort,
                        LOAD_BALANCER_LISTENER.ACCOUNT_ID,
                        service.getAccountId(), LOAD_BALANCER_LISTENER.SOURCE_PORT, publicPort,
                        LOAD_BALANCER_LISTENER.TARGET_PORT, privatePort,
                        LOAD_BALANCER_LISTENER.SOURCE_PROTOCOL, spec.getProtocol(),
                        LOAD_BALANCER_LISTENER.TARGET_PROTOCOL,
                        spec.getProtocol(),
                        LoadBalancerConstants.FIELD_LB_LISTENER_ALGORITHM, "roundrobin",
                        LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId(),
                        LOAD_BALANCER_LISTENER.SERVICE_ID, service.getId());
            }
            objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_LISTENER_CREATE, listenerObj, null);

            listeners.put(publicPort, listenerObj);
        }

        for (LoadBalancerListener listener : listeners.values()) {
            lbService.addListenerToConfig(lbConfig, listener.getId());
        }
    }
}
