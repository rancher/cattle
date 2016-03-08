package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.SERVICE_INDEX;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import static io.cattle.platform.core.model.tables.SubnetTable.SUBNET;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.FrameworkEvents;
import io.cattle.platform.framework.event.util.EventUtils;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.lock.HostEndpointsUpdateLock;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.servicediscovery.deployment.impl.lock.ServiceEndpointsUpdateLock;
import io.cattle.platform.servicediscovery.deployment.impl.lock.StackHealthStateUpdateLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

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
    ServiceExposeMapDao exposeMapDao;

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ResourcePoolManager poolManager;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    LabelsDao labelsDao;

    @Inject
    LockManager lockManager;

    @Inject
    AllocatorService allocatorService;

    @Inject
    EventService eventService;

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

    public List<Integer> getServiceInstanceUsedSuffixes(Service service, String launchConfigName) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        // get all existing instances to check if the name is in use by the instance of the same service
        List<Integer> usedSuffixes = new ArrayList<>();
        List<? extends Instance> serviceInstances = exposeMapDao.listServiceManagedInstances(service, launchConfigName);
        for (Instance instance : serviceInstances) {
            // exclude unhealthy instances as they are going to be replaced
            if (StringUtils.equals(instance.getHealthState(), HealthcheckConstants.HEALTH_STATE_UNHEALTHY)
                    || StringUtils.equals(instance.getHealthState(),
                            HealthcheckConstants.HEALTH_STATE_UPDATING_UNHEALTHY)) {
                continue;
            }
            if (ServiceDiscoveryUtil.isServiceGeneratedName(env, service, instance.getName())) {
                // legacy code - to support old data where service suffix wasn't set
                String configName = launchConfigName == null
                        || launchConfigName.equals(ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME) ? ""
                        : launchConfigName + "_";
                
                String id = instance.getName().replace(String.format("%s_%s_%s", env.getName(), service.getName(), configName), "");
                if (id.matches("\\d+")) {
                    usedSuffixes.add(Integer.valueOf(id));
                }
            }
        }
        return usedSuffixes;
    }

    protected String getLoadBalancerName(Service service) {
        Environment env = objectManager.findOne(Environment.class, ENVIRONMENT.ID, service.getEnvironmentId());
        return String.format("%s_%s", env.getName(), service.getName());
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
    public boolean isActiveService(Service service) {
        return (getServiceActiveStates().contains(service.getState()));
    }

    @Override
    public List<String> getServiceActiveStates() {
        return Arrays.asList(CommonStatesConstants.ACTIVATING,
                CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE,
                ServiceDiscoveryConstants.STATE_UPGRADING, ServiceDiscoveryConstants.STATE_ROLLINGBACK,
                ServiceDiscoveryConstants.STATE_CANCELING_UPGRADE,
                ServiceDiscoveryConstants.STATE_CANCELED_UPGRADE,
                ServiceDiscoveryConstants.STATE_CANCELING_ROLLBACK,
                ServiceDiscoveryConstants.STATE_CANCELED_ROLLBACK,
                ServiceDiscoveryConstants.STATE_FINISHING_UPGRADE,
                ServiceDiscoveryConstants.STATE_UPGRADED,
                ServiceDiscoveryConstants.STATE_RESTARTING);
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

    protected String allocateVip(Service service) {
        if (service.getKind().equalsIgnoreCase(KIND.LOADBALANCERSERVICE.name())
                || service.getKind().equalsIgnoreCase(KIND.SERVICE.name())
                || service.getKind().equalsIgnoreCase(KIND.DNSSERVICE.name())) {
            Subnet vipSubnet = getServiceVipSubnet(service);
            String requestedVip = service.getVip();
            return allocateIpForService(service, vipSubnet, requestedVip);
        }
        return null;
    }

    @Override
    public String allocateIpForService(Object owner, Subnet subnet, String requestedIp) {
        PooledResourceOptions options = new PooledResourceOptions();
        if (requestedIp != null) {
            options.setRequestedItem(requestedIp);
        }
        PooledResource resource = poolManager.allocateOneResource(subnet, owner, options);
        if (resource != null) {
            return resource.getName();
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
        if (!(DataAccessor.fieldBool(service, ServiceDiscoveryConstants.FIELD_SET_VIP) || service.getVip() != null)) {
            return;
        }
        String vip = allocateVip(service);
        if (vip != null) {
            service.setVip(vip);
            objectManager.persist(service);
        }
    }

    @SuppressWarnings("unchecked")
    private List<PortSpec> getServicePorts(Service service, Map<String, Object> launchConfigData) {
        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) == null) {
            return new ArrayList<>();
        }
        List<String> specs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        List<PortSpec> ports = new ArrayList<>();
        for (String spec : specs) {
            boolean defaultProtocol = true;
            if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
                defaultProtocol = false;
            }
            ports.add(new PortSpec(spec, defaultProtocol));
        }
        return ports;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setPorts(Service service) {
        Account env = objectManager.loadResource(Account.class, service.getAccountId());
        List<PooledResource> allocatedPorts = allocatePorts(env, service);
        // update primary launchConfig
        Map<String, Object> launchConfig = DataAccessor.fields(service)
                .withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);

        setRandomPublicPorts(env, service, launchConfig, allocatedPorts);

        // update secondary launch configs
        List<Object> secondaryLaunchConfigs = DataAccessor.fields(service)
                .withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class);
        for (Object secondaryLaunchConfig : secondaryLaunchConfigs) {
            setRandomPublicPorts(env, service, (Map<String, Object>) secondaryLaunchConfig, allocatedPorts);
        }

        DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_LAUNCH_CONFIG).set(launchConfig);
        DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .set(secondaryLaunchConfigs);
        objectManager.persist(service);
    }

    @SuppressWarnings("unchecked")
    protected List<PooledResource> allocatePorts(Account env, Service service) {
        int toAllocate = 0;
        for (String launchConfigName : ServiceDiscoveryUtil.getServiceLaunchConfigNames(service)) {
            Object ports = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                    InstanceConstants.FIELD_PORTS);
            if (ports != null) {
                for (String port : (List<String>) ports) {
                    if (new PortSpec(port).getPublicPort() == null) {
                        toAllocate++;
                    }
                }
            }
        }
        List<PooledResource> resource = poolManager.allocateResource(env, service,
                new PooledResourceOptions().withCount(toAllocate).withQualifier(
                        ResourcePoolConstants.ENVIRONMENT_PORT));
        if (resource == null) {
            resource = new ArrayList<>();
        }
        return resource;
    }

    protected void setRandomPublicPorts(Account env, Service service,
            Map<String, Object> launchConfigData, List<PooledResource> allocatedPorts) {
        List<PortSpec> ports = getServicePorts(service, launchConfigData);
        List<String> newPorts = new ArrayList<>();
        List<PortSpec> toAllocate = new ArrayList<>();
        for (PortSpec port : ports) {
            if (port.getPublicPort() == null) {
                toAllocate.add(port);
            } else {
                newPorts.add(port.toSpec());
            }
        }
        
        for (PortSpec port : toAllocate) {
            if (!allocatedPorts.isEmpty()) {
                port.setPublicPort(new Integer(allocatedPorts.get(0).getName()));
                allocatedPorts.remove(0);
            }
            newPorts.add(port.toSpec());
        }

        if (!newPorts.isEmpty()) {
            launchConfigData.put(InstanceConstants.FIELD_PORTS, newPorts);
        }
    }

    @Override
    public void releasePorts(Service service) {
        Account account = objectManager.loadResource(Account.class, service.getAccountId());
        poolManager.releaseResource(account, service, new PooledResourceOptions().withQualifier(
                ResourcePoolConstants.ENVIRONMENT_PORT));
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
        if (StringUtils.isBlank(selector)) {
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
        String selector = sourceService.getSelectorContainer();
        if (StringUtils.isBlank(selector)) {
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

    @Override
    public boolean isGlobalService(Service service) {
        Map<String, String> serviceLabels = ServiceDiscoveryUtil.getServiceLabels(service, allocatorService);
        String globalService = serviceLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_GLOBAL);
        if (globalService != null && Boolean.valueOf(globalService).equals(true)) {
            return true;
        }
        return false;
    }

    protected void updateObjectEndPoint(final Object object, final String resourceType, final Long resourceId,
            final PublicEndpoint publicEndpoint, final boolean add) {
        // have to reload the object to get the latest update for publicEndpoint
        // if don't reload, its possible that n concurrent updates would lack information.
        // update would be performed on the original object

        if (publicEndpoint.getPort() == null || publicEndpoint.getIpAddress() == null) {
            return;
        }
        Object reloaded = objectManager.reload(object);
        List<PublicEndpoint> publicEndpoints = new ArrayList<>();
        publicEndpoints.addAll(DataAccessor.fields(reloaded)
                .withKey(ServiceDiscoveryConstants.FIELD_PUBLIC_ENDPOINTS)
                .withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, PublicEndpoint.class));

        if (publicEndpoints.contains(publicEndpoint) != add) {
            if (add) {
                publicEndpoints.add(publicEndpoint);
            } else {
                publicEndpoints.remove(publicEndpoint);
            }
            DataUtils.getWritableFields(object).put(ServiceDiscoveryConstants.FIELD_PUBLIC_ENDPOINTS,
                    publicEndpoints);
            objectManager.persist(object);
            final Map<String, Object> data = new HashMap<>();
            data.put(ServiceDiscoveryConstants.FIELD_PUBLIC_ENDPOINTS, publicEndpoints);

            DeferredUtils.nest(new Runnable() {
                @Override
                public void run() {
                    EventUtils.TriggerStateChanged(eventService, resourceId.toString(), resourceType, data);
                }
            });
        }
    }

    @Override
    public void propagatePublicEndpoint(final PublicEndpoint publicEndpoint, final boolean add) {
        final Host host = publicEndpoint.getHost();
        if (host.getRemoved() == null) {
            lockManager.lock(new HostEndpointsUpdateLock(host),
                    new LockCallbackNoReturn() {
                        @Override
                        public void doWithLockNoResult() {
                            updateObjectEndPoint(host, host.getKind(), Long.valueOf(publicEndpoint.getHostId()),
                                    publicEndpoint, add);
                        }
                    });
        }

        final Service service = publicEndpoint.getService();
        lockManager.lock(new ServiceEndpointsUpdateLock(service), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                updateObjectEndPoint(service, service.getKind(), service.getId(), publicEndpoint, add);
            }
        });
    }

    @Override
    public void setToken(Service service) {
        String token = ApiKeyFilter.generateKeys()[1];
        DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_TOKEN).set(token);
        objectManager.persist(service);
    }

    @Override
    public void removeServiceIndexes(Service service) {
        for (ServiceIndex serviceIndex : objectManager.find(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID, service.getId(),
                SERVICE_INDEX.REMOVED, null)) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, serviceIndex, null);
        }
    }

    @Override
    public void allocateIpToServiceIndex(ServiceIndex serviceIndex, String requestedIp) {
        if (StringUtils.isEmpty(serviceIndex.getAddress())) {
            Network ntwk = ntwkDao.getNetworkForObject(serviceIndex, NetworkConstants.KIND_HOSTONLY);
            if (ntwk != null) {
                Subnet subnet = ntwkDao.addManagedNetworkSubnet(ntwk);
                String ipAddress = allocateIpForService(serviceIndex, subnet, requestedIp);
                serviceIndex.setAddress(ipAddress);
                objectManager.persist(serviceIndex);
            }
        }
    }

    @Override
    public void releaseIpFromServiceIndex(ServiceIndex serviceIndex) {
        if (!StringUtils.isEmpty(serviceIndex.getAddress())) {
            Network ntwk = ntwkDao.getNetworkForObject(serviceIndex, NetworkConstants.KIND_HOSTONLY);
            if (ntwk != null) {
                Subnet subnet = ntwkDao.addManagedNetworkSubnet(ntwk);
                poolManager.releaseResource(subnet, serviceIndex);
            }
        }
    }

    @Override
    public void updateHealthState(final List<? extends Service> services) {
        if (services == null || services.size() == 0) {
            return;
        }
        // assuming instance can't belong to more than one stack
        // (possible only for instances joining service by selector)
        final Environment stack = objectManager.loadResource(Environment.class, services.get(0).getEnvironmentId());
        lockManager.lock(new StackHealthStateUpdateLock(stack), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                // modify service health state
                setServiceHealthState(services);

                setStackHealthState(stack);
            }

            protected void setStackHealthState(final Environment stack) {
                String stackHealthState = getStackHealthState(stack);

                Map<String, Object> fields = new HashMap<>();
                        fields.put("healthState", stackHealthState);
                        objectManager.setFields(stack, fields);
                publishEvent(stack.getAccountId(), stack.getId(), stack.getKind());
            }

            protected String getStackHealthState(final Environment stack) {
                List<Service> services = objectManager.find(Service.class, SERVICE.ENVIRONMENT_ID, stack.getId(),
                        SERVICE.REMOVED, null);

                int init = 0;
                int healthy = 0;
                int expectedCount = 0;
                List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVE.toLowerCase(),
                        CommonStatesConstants.ACTIVATING.toLowerCase());
                List<String> healthyStates = Arrays.asList(
                        HealthcheckConstants.SERVICE_HEALTH_STATE_STARTED_ONCE.toLowerCase(),
                        HealthcheckConstants.HEALTH_STATE_HEALTHY.toLowerCase());
                List<String> ignoreStates = Arrays.asList(CommonStatesConstants.REMOVING,
                        CommonStatesConstants.REMOVED, CommonStatesConstants.PURGED, CommonStatesConstants.PURGING);
                for (Service service : services) {
                    String sHS = service.getHealthState() == null ? HealthcheckConstants.HEALTH_STATE_HEALTHY : service.getHealthState().toLowerCase();
                    if (ignoreStates.contains(service.getState())) {
                        continue;
                    }
                    expectedCount++;
                    if (activeStates.contains(service.getState().toLowerCase()) && healthyStates.contains(sHS)) {
                        healthy++;
                    } else if (sHS.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_INITIALIZING)) {
                        init++;
                    }
                }

                String stackHealthState = HealthcheckConstants.HEALTH_STATE_UNHEALTHY;
                if (healthy >= expectedCount) {
                    stackHealthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
                } else if (init > 0) {
                    stackHealthState = HealthcheckConstants.HEALTH_STATE_INITIALIZING;
                } else if (healthy > 0) {
                    stackHealthState = HealthcheckConstants.SERVICE_HEALTH_STATE_DEGRADED;
                }
                return stackHealthState;
            }

            protected void setServiceHealthState(final List<? extends Service> services) {
                for (Service service : services) {
                    String serviceHealthState = getServiceHealthState(service);

                    Map<String, Object> fields = new HashMap<>();
                    fields.put("healthState", serviceHealthState);
                    objectManager.setFields(service, fields);
                    publishEvent(service.getAccountId(), service.getId(), service.getKind());
                }
            }

            protected String getServiceHealthState(Service service) {
                String serviceHealthState = null;
                List<String> supportedKinds = Arrays.asList(
                        ServiceDiscoveryConstants.KIND.SERVICE.name().toLowerCase(),
                        ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name().toLowerCase());
                if (!supportedKinds.contains(service.getKind().toLowerCase())) {
                    serviceHealthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
                } else {
                    List<? extends Instance> serviceInstances = exposeMapDao.listServiceManagedInstances(service
                            .getId());
                    List<String> healthyStates = Arrays.asList(HealthcheckConstants.HEALTH_STATE_HEALTHY,
                            HealthcheckConstants.HEALTH_STATE_UPDATING_HEALTHY);
                    List<String> initStates = Arrays.asList(HealthcheckConstants.HEALTH_STATE_INITIALIZING,
                            HealthcheckConstants.HEALTH_STATE_REINITIALIZING);

                    Integer scale = DataAccessor.fieldInteger(service, ServiceDiscoveryConstants.FIELD_SCALE);
                    if (scale == null || ServiceDiscoveryUtil.isNoopService(service, allocatorService)) {
                        scale = 0;
                    }
                    List<String> lcs = ServiceDiscoveryUtil.getServiceLaunchConfigNames(service);
                    Integer expectedScale = scale * lcs.size();
                    boolean isGlobal = isGlobalService(service);
                    int healthyCount = 0;
                    int initCount = 0;
                    int instanceCount = serviceInstances.size();
                    int startedOnce = 0;
                    List<String> runningStates = Arrays.asList(InstanceConstants.STATE_RUNNING);
                    for (Instance instance : serviceInstances) {
                        String iHS = instance.getHealthState() == null ? HealthcheckConstants.HEALTH_STATE_HEALTHY : instance.getHealthState().toLowerCase();
                        if (runningStates.contains(instance.getState().toLowerCase())) {
                            if (healthyStates.contains(iHS)) {
                                healthyCount++;
                            } else if (initStates.contains(iHS)) {
                                initCount++;
                            }
                        }
                        
                        if (isStartOnce(instance)) {
                            startedOnce++;
                        }
                    }

                    if (startedOnce > 0 && startedOnce == expectedScale) {
                        return HealthcheckConstants.SERVICE_HEALTH_STATE_STARTED_ONCE;
                    }

                    if ((isGlobal && healthyCount >= instanceCount && instanceCount > 0)
                            || (!isGlobal && healthyCount >= expectedScale)) {
                        return HealthcheckConstants.HEALTH_STATE_HEALTHY;
                    } else if (initCount > 0) {
                        serviceHealthState = HealthcheckConstants.HEALTH_STATE_INITIALIZING;
                    } else if (healthyCount > 0) {
                        serviceHealthState = HealthcheckConstants.SERVICE_HEALTH_STATE_DEGRADED;
                    } else {
                        serviceHealthState = HealthcheckConstants.HEALTH_STATE_UNHEALTHY;
                    }
                }
                return serviceHealthState;
            }

            protected boolean isStartOnce(Instance instance) {
                Map<String, Object> labels = DataAccessor
                        .fieldMap(instance, InstanceConstants.FIELD_LABELS);
                boolean startOnce = false;
                if (labels.containsKey(ServiceDiscoveryConstants.LABEL_SERVICE_CONTAINER_START_ONCE)) {
                    startOnce = Boolean.valueOf(((String) labels
                            .get(ServiceDiscoveryConstants.LABEL_SERVICE_CONTAINER_START_ONCE)));
                }
                return startOnce;
            }
        });
    }

    protected void publishEvent(long accountId, long resourceId, String resourceType) {
        Map<String, Object> data = new HashMap<>();
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);

        Event event = EventVO.newEvent(FrameworkEvents.STATE_CHANGE)
                .withData(data)
                .withResourceType(resourceType)
                .withResourceId(String.valueOf(resourceId));

        eventService.publish(event);
    }
}
