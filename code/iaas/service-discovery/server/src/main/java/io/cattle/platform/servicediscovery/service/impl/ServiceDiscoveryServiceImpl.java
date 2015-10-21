package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.LoadBalancerConfigTable.LOAD_BALANCER_CONFIG;
import static io.cattle.platform.core.model.tables.LoadBalancerListenerTable.LOAD_BALANCER_LISTENER;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import static io.cattle.platform.core.model.tables.SubnetTable.SUBNET;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

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

    @Inject
    ResourcePoolManager poolManager;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    LabelsDao labelsDao;

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
        List<? extends Instance> serviceInstances = exposeMapDao.listServiceManagedInstances(service.getId());
        
        for (Instance instance : serviceInstances) {
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
    public void createLoadBalancerService(Service service, List<? extends Long> certIds, Long defaultCertId) {
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
        createLoadBalancer(service, lbName, lbConfig, launchConfigData, certIds, defaultCertId);

    }
    
    private void createLoadBalancer(Service service, String lbName, LoadBalancerConfig lbConfig,
            Map<String, Object> launchConfigData, List<? extends Long> certIds, Long defaultCertId) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null, LOAD_BALANCER.ACCOUNT_ID, service.getAccountId());
        Map<String, Object> data = populateLBData(service, lbName, lbConfig, launchConfigData, certIds, defaultCertId);
        if (lb == null) {
            lb = objectManager.create(LoadBalancer.class, data);
        }

        objectProcessManager.executeProcess(LoadBalancerConstants.PROCESS_LB_CREATE, lb, data);
    }


    protected Map<String, Object> populateLBData(Service service, String lbName, LoadBalancerConfig lbConfig,
            Map<String, Object> launchConfigData, List<? extends Long> certIds, Long defaultCertId) {
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
        if (defaultCertId != null) {
            data.put(LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID, defaultCertId);
        }

        if (certIds != null) {
            data.put(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS, certIds);
        }
        return data;
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
        
        List<String> sslPorts = getSslPorts(launchConfigData);

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

            int targetPort = spec.getPrivatePort();
            Integer sourcePort = null;
            Integer privatePort = null;
            // set sourcePort only for ports defined in "ports" param
            // the ones defined in expose, will get translated to private listeners
            if (portDefs.get(port)) {
                if (spec.getPublicPort() == null) {
                    sourcePort = targetPort;
                } else {
                    sourcePort = spec.getPublicPort();
                }
                privatePort = sourcePort;
            } else {
                if (spec.getPublicPort() == null) {
                    privatePort = targetPort;
                } else {
                    privatePort = spec.getPublicPort();
                }
            }
            
            String sourceProtocol = protocol;
            if (sslPorts.contains(privatePort.toString())) {
                if (protocol.equals("tcp")) {
                    sourceProtocol = "ssl";
                } else {
                    sourceProtocol = "https";
                }
            }
            
            createListener(service, listeners, new LoadBalancerListenerPort(privatePort, sourcePort,
                    sourceProtocol, targetPort));
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


    @SuppressWarnings("unchecked")
    protected List<String> getSslPorts(Map<String, Object> launchConfigData) {
        List<String> sslPorts = new ArrayList<>();
        Map<String, String> labels = (Map<String, String>) launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            Object sslPortsObj = labels.get(ServiceDiscoveryConstants.LABEL_LB_SSL_PORTS);
            if (sslPortsObj != null) {
                for (String sslPort : sslPortsObj.toString().split(",")) {
                    sslPorts.add(sslPort.trim());
                }
            }
        }

        return sslPorts;
    }

    @Override
    public boolean isActiveService(Service service) {
        return (getServiceActiveStates(true).contains(service.getState()));
    }

    @Override
    public List<String> getServiceActiveStates(boolean includeUpgrading) {
        if (includeUpgrading) {
            return Arrays.asList(CommonStatesConstants.ACTIVATING,
                    CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE,
                    ServiceDiscoveryConstants.STATE_UPGRADING, ServiceDiscoveryConstants.STATE_ROLLINGBACK,
                    ServiceDiscoveryConstants.STATE_CANCELING_UPGRADE,
                    ServiceDiscoveryConstants.STATE_CANCELED_UPGRADE,
                    ServiceDiscoveryConstants.STATE_CANCELING_ROLLBACK,
                    ServiceDiscoveryConstants.STATE_CANCELED_ROLLBACK,
                    ServiceDiscoveryConstants.STATE_FINISHING_UPGRADE,
                    ServiceDiscoveryConstants.STATE_UPGRADED);
        } else {
            return Arrays.asList(CommonStatesConstants.ACTIVATING,
                    CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE);
        }
    }

    @Override
    public void addToLoadBalancerService(Service lbSvc, ServiceExposeMap instanceToRegister) {
        if (!lbSvc.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
            return;
        }
        
        if (!isActiveService(lbSvc)) {
            return;
        }

        // register only instances of primary service
        if (instanceToRegister.getDnsPrefix() != null) {
            return;
        }

        if (!exposeMapDao.isActiveMap(instanceToRegister)) {
            return;
        }

        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID,
                lbSvc.getId(), LOAD_BALANCER.REMOVED, null);
        if (lb == null) {
            return;
        }

        ServiceConsumeMap map = consumeMapDao.findNonRemovedMap(lbSvc.getId(), instanceToRegister.getServiceId(),
                null);
        if (map == null) {
            return;
        }

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

    protected String getServiceVIP(Service service, String requestedVip) {
        if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())
                || service.getKind().equalsIgnoreCase(KIND.SERVICE.name())
                || service.getKind().equalsIgnoreCase(KIND.DNSSERVICE.name())) {
            Subnet vipSubnet = getServiceVipSubnet(service);
            PooledResourceOptions options = new PooledResourceOptions();
            if (requestedVip != null) {
                options.setRequestedItem(requestedVip);
            }
            PooledResource resource = poolManager.allocateOneResource(vipSubnet, service, options);
            if (resource != null) {
                return resource.getName();
            }
        }
        return null;
    }

    protected Subnet getServiceVipSubnet(final Service service) {
        Subnet vipSubnet = DeferredUtils.nest(new Callable<Subnet>() {
            @Override
            public Subnet call() throws Exception {
                return ntwkDao.addVIPSubnet(service.getAccountId());
            }
        });

        // wait for subnet to become active so the ip range is populated
        vipSubnet = resourceMonitor.waitFor(vipSubnet,
                new ResourcePredicate<Subnet>() {
                    @Override
                    public boolean evaluate(Subnet obj) {
                        return CommonStatesConstants.ACTIVE.equals(obj.getState());
                    }
                });
        return vipSubnet;
    }

    @Override
    public void setVIP(Service service) {
        String requestedVip = service.getVip();
        String vip = getServiceVIP(service, requestedVip);
        if (vip != null || requestedVip != null) {
            service.setVip(vip);
            objectManager.persist(service);
        }
    }

    @Override
    public void releaseVip(Service service) {
        String vip = service.getVip();
        if (vip == null) {
            return;
        }
        List<Subnet> subnets = objectManager.find(Subnet.class, SUBNET.ACCOUNT_ID, service.getAccountId(), SUBNET.KIND,
                SubnetConstants.KIND_VIP_SUBNET);
        if (subnets.isEmpty()) {
            return;
        }
        Subnet subnet = subnets.get(0);
        poolManager.releaseResource(subnet, service);
    }

    @Override
    public void updateLoadBalancerService(Service service, List<? extends Long> certIds, Long defaultCertId) {
        LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.SERVICE_ID, service.getId(),
                LOAD_BALANCER.REMOVED, null);
        if (lb != null) {
            Map<String, Object> data = new HashMap<>();
            if (certIds == null) {
                certIds = DataAccessor.fields(service)
                        .withKey(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS).asList(jsonMapper, Long.class);
            }
            if (defaultCertId == null) {
                defaultCertId = DataAccessor.fieldLong(service, LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
            }
            data.put(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS, certIds);
            data.put(LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID, defaultCertId);
            DataUtils.getWritableFields(lb).putAll(data);
            objectManager.persist(lb);
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, lb, data);
        }
    }

    @Override
    public void addServiceLink(final Service service, final ServiceLink serviceLink) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                consumeMapDao.createServiceLink(service, serviceLink);
            }
        });
    }

    @Override
    public boolean isSelectorLinkMatch(Service sourceService, Service targetService) {
        String selector = sourceService.getSelectorLink();
        if (selector == null) {
            return false;
        }
        Map<String, String> serviceLabels = ServiceDiscoveryUtil.getLaunchConfigLabels(targetService, ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        if (serviceLabels.isEmpty()) {
            return false;
        }
        return SelectorUtils.isSelectorMatch(selector, serviceLabels);

    }

    @Override
    public boolean isSelectorContainerMatch(Service sourceService, long instanceId) {
        if (!sourceService.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.SERVICE.name())) {
            return false;
        }
        String selector = sourceService.getSelectorContainer();
        if (selector == null) {
            return false;
        }
        List<? extends Label> labels = labelsDao.getLabelsForInstance(instanceId);
        if (labels.isEmpty()) {
            return false;
        }
        Map<String, String> instanceLabels = new HashMap<>();
        for (Label label : labels) {
            instanceLabels.put(label.getKey(), label.getValue());
        }
        
        return SelectorUtils.isSelectorMatch(selector, instanceLabels);
    }

    @Override
    public boolean isServiceInstance(Service service, Instance instance) {
        return exposeMapDao.getServiceInstanceMap(service, instance) != null;
    }
}
