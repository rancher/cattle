package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import static io.cattle.platform.core.model.tables.LoadBalancerConfigTable.*;
import static io.cattle.platform.core.model.tables.LoadBalancerListenerTable.*;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.*;

import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class ServiceDiscoveryServiceImpl implements ServiceDiscoveryService {

    @Inject
    ServiceConsumeMapDao consumeMapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    LoadBalancerService lbService;

    @Inject
    ServiceExposeMapDao exposeMapDao;

    @Inject
    JsonMapper jsonMapper;

    protected long getServiceNetworkId(Service service) {
        Network network = ntwkDao.getNetworkForObject(service, NetworkConstants.KIND_HOSTONLY);
        if (network == null) {
            throw new RuntimeException(
                    "Unable to find a network to activate a service " + service.getId());
        }
        long networkId = network.getId();
        return networkId;
    }


    @Override
    public List<Integer> getServiceInstanceUsedOrderIds(Service service, String launchConfigName) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        // get all existing instances to check if the name is in use by the instance of the same service
        List<Integer> usedIds = new ArrayList<>();
        // list all the instances
        List<? extends ServiceExposeMap> instanceServiceMaps = exposeMapDao.getNonRemovedServiceInstanceMap(service
                .getId());
        
        for (ServiceExposeMap instanceServiceMap : instanceServiceMaps) {
            Instance instance = objectManager.loadResource(Instance.class, instanceServiceMap.getInstanceId());
            if (ServiceDiscoveryUtil.isServiceGeneratedName(env, service, instance)) {
                
                String configName = launchConfigName == null
                        || launchConfigName.equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? ""
                        : launchConfigName + "_";
                
                String id = instance.getName().replace(String.format("%s_%s_%s", env.getName(), service.getName(), configName), "");
                if (id.matches("\\d+")) {
                    usedIds.add(Integer.valueOf(id));
                }
            }
        }
        return usedIds;
    }

    protected String getLoadBalancerName(Service service) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return String.format("%s_%s", env.getName(), service.getName());
    }

    @Override
    public void cleanupLoadBalancerService(Service service) {
        // 1) remove load balancer
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);
        if (lb != null) {
            objectProcessManager.scheduleProcessInstance(LoadBalancerConstants.PROCESS_LB_REMOVE, lb, null);
        }
    }

    @Override
    public void removeServiceMaps(Service service) {
        // 1. remove all maps to the services consumed by service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumedMapsToRemove(service.getId())) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }

        // 2. remove all maps to the services consuming service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumingMapsToRemove(service.getId())) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createLoadBalancerService(Service service) {
        String lbName = getLoadBalancerName(service);
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
        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service, null);
        createListeners(service, lbConfig, launchConfigData);

        // 3. create a load balancer
        createLoadBalancer(service, lbName, lbConfig);
    }

    private void createLoadBalancer(Service service, String lbName, LoadBalancerConfig lbConfig) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null, LOAD_BALANCER.ACCOUNT_ID, service.getAccountId());
        if (lb == null) {
            Map<String, Object> launchConfigData = ServiceDiscoveryUtil.getLaunchConfigDataAsMap(service, null);
            Map<String, Object> data = new HashMap<>();
            data.put("name", lbName);
            data.put(LoadBalancerConstants.FIELD_LB_CONFIG_ID, lbConfig.getId());
            data.put(LoadBalancerConstants.FIELD_LB_SERVICE_ID, service.getId());
            data.put(LoadBalancerConstants.FIELD_LB_NETWORK_ID, getServiceNetworkId(service));
            data.put(
                    LoadBalancerConstants.FIELD_LB_INSTANCE_IMAGE_UUID,
                    launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID));
            data.put(
                    LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE,
                    DataAccessor.fields(service).withKey(LoadBalancerConstants.FIELD_LB_INSTANCE_URI_PREDICATE)
                            .withDefault("delegate:///").as(
                                    String.class));
            data.put("accountId", service.getAccountId());
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
        Map<Integer, LoadBalancerListener> listeners = new HashMap<>();

        // 1. create listeners
        Map<String, Boolean> portDefs = new HashMap<>();

        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) != null) {
            for (String port : (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS)) {
                portDefs.put(port, true);
            }
        }

        if (launchConfigData.get(InstanceConstants.FIELD_EXPOSE) != null) {
            for (String port : (List<String>) launchConfigData.get(InstanceConstants.FIELD_EXPOSE)) {
                portDefs.put(port, false);
            }
        }

        for (String port : portDefs.keySet()) {
            PortSpec spec = new PortSpec(port);
            String protocol;
            if (!port.contains("tcp")) {
                // default to http unless defined otherwise in the compose file
                protocol = "http";
            } else {
                protocol = "tcp";
            }

            if (listeners.containsKey(spec.getPrivatePort())) {
                continue;
            }

            int privatePort = spec.getPrivatePort();
            Integer sourcePort = spec.getPublicPort();
            // set sourcePort only for ports defined in "ports" param
            // the ones defined in expose, will get translated to private listeners
            if (portDefs.get(port) && sourcePort == null) {
                sourcePort = privatePort;
            }

            createListener(service, listeners, new LoadBalancerListenerPort(privatePort, sourcePort,
                    protocol, privatePort));
        }

        for (LoadBalancerListener listener : listeners.values()) {
            lbService.addListenerToConfig(lbConfig, listener.getId());
        }
    }
    
    private class LoadBalancerListenerPort {
        int privatePort;
        Integer sourcePort;
        Integer targetPort;
        String protocol;

        public LoadBalancerListenerPort(int privatePort, Integer sourcePort, String protocol, Integer targetPort) {
            super();
            this.privatePort = privatePort;
            this.sourcePort = sourcePort;
            this.protocol = protocol;
            this.targetPort = targetPort;
        }

        public int getPrivatePort() {
            return privatePort;
        }

        public String getProtocol() {
            return protocol;
        }

        public Integer getSourcePort() {
            return sourcePort;
        }

        public Integer getTargetPort() {
            return targetPort;
        }
    }


    protected void createListener(Service service, Map<Integer, LoadBalancerListener> listeners,
            LoadBalancerListenerPort port) {
        LoadBalancerListener listenerObj = objectManager.findOne(LoadBalancerListener.class,
                LOAD_BALANCER_LISTENER.SERVICE_ID, service.getId(),
                LOAD_BALANCER_LISTENER.SOURCE_PORT, port.getSourcePort(),
                LOAD_BALANCER_LISTENER.PRIVATE_PORT, port.getPrivatePort(),
                LOAD_BALANCER_LISTENER.TARGET_PORT, port.getTargetPort(),
                LOAD_BALANCER_LISTENER.REMOVED, null,
                LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId());

        if (listenerObj == null) {
            listenerObj = objectManager.create(LoadBalancerListener.class,
                    LOAD_BALANCER_LISTENER.NAME, getLoadBalancerName(service) + "_" + port.getPrivatePort(),
                    LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId(),
                    LOAD_BALANCER_LISTENER.SOURCE_PORT, port.getSourcePort(),
                    LOAD_BALANCER_LISTENER.PRIVATE_PORT, port.getPrivatePort(),
                    LOAD_BALANCER_LISTENER.TARGET_PORT, port.getTargetPort(),
                    LOAD_BALANCER_LISTENER.SOURCE_PROTOCOL, port.getProtocol(),
                    LOAD_BALANCER_LISTENER.TARGET_PROTOCOL, port.getProtocol(),
                    LoadBalancerConstants.FIELD_LB_LISTENER_ALGORITHM, "roundrobin",
                    LOAD_BALANCER_LISTENER.ACCOUNT_ID, service.getAccountId(),
                    LOAD_BALANCER_LISTENER.SERVICE_ID, service.getId());
        }
        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_LISTENER_CREATE, listenerObj, null);

        listeners.put(listenerObj.getPrivatePort(), listenerObj);
    }

    @Override
    public boolean isActiveService(Service service) {
        List<String> validStates = Arrays.asList(CommonStatesConstants.ACTIVATING,
                CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE,
                ServiceDiscoveryConstants.STATE_UPGRADING);
        return (validStates.contains(service.getState()));
    }


    @Override
    public void addToLoadBalancerService(Service lbSvc, ServiceExposeMap instanceToRegister) {
        if (!lbSvc.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return;
        }
        
        if (!isActiveService(lbSvc)) {
            return;
        }

        if (!exposeMapDao.isActiveMap(instanceToRegister)) {
            return;
        }

        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                lbSvc.getId(), LOAD_BALANCER.REMOVED, null);
        ServiceConsumeMap map = consumeMapDao.findNonRemovedMap(lbSvc.getId(), instanceToRegister.getServiceId(),
                null);
        LoadBalancerTargetInput target = new LoadBalancerTargetInput(instanceToRegister,
                map, jsonMapper);
        lbService.addTargetToLoadBalancer(lb, target);

    }

    @Override
    public void cloneConsumingServices(Service fromService, Service toService) {
        List<ServiceLink> linksToCreate = new ArrayList<>();

        for (ServiceConsumeMap map : consumeMapDao.findConsumingServices(fromService.getId())) {
            ServiceLink link;
            List<String> ports = DataAccessor.fieldStringList(map, LoadBalancerConstants.FIELD_LB_TARGET_PORTS);
            if (ports == null) {
                link = new ServiceLink(toService.getId(), map.getName());
            } else {
                link = new LoadBalancerServiceLink(toService.getId(), map.getName(), ports);
            }

            link.setConsumingServiceId(map.getServiceId());
            linksToCreate.add(link);
        }

        consumeMapDao.createServiceLinks(linksToCreate);
    }

}
