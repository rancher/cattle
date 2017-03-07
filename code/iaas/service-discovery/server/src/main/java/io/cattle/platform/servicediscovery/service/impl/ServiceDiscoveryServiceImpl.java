package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.AccountLinkTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;
import io.cattle.platform.allocator.service.AllocationHelper;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.AccountLink;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.network.IPAssignment;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.servicediscovery.api.lock.CreateServiceLock;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.servicediscovery.deployment.impl.lock.LoadBalancerServiceLock;
import io.cattle.platform.servicediscovery.deployment.impl.lock.StackVolumeLock;
import io.cattle.platform.servicediscovery.service.DeploymentManager;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.upgrade.UpgradeManager;
import io.cattle.platform.util.exception.DeploymentUnitAllocateException;
import io.cattle.platform.util.exception.ResourceExhaustionException;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicIntProperty;

public class ServiceDiscoveryServiceImpl implements ServiceDiscoveryService {

    private static final String HOST_ENDPOINTS_UPDATE = "host-endpoints-update";
    private static final String SERVICE_ENDPOINTS_UPDATE = "service-endpoints-update";

    private static final DynamicIntProperty EXECUTION_MAX = ArchaiusUtil.getInt("service.execution.credits");
    private static final DynamicIntProperty EXECUTION_PERIOD = ArchaiusUtil.getInt("service.execution.period.seconds");
    private static final DynamicIntProperty EXECUTION_DELAY = ArchaiusUtil.getInt("service.execution.delay.seconds");

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
    LockManager lockManager;
    @Inject
    AllocationHelper allocationHelper;
    @Inject
    EventService eventService;
    @Inject
    InstanceDao instanceDao;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    NetworkService networkService;
    @Inject
    ServiceDao serviceDao;
    @Inject
    UpgradeManager upgradeMgr;
    @Inject
    DeploymentManager deploymentMgr;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public void removeServiceLinks(Service service) {
        // 1. remove all maps to the services consumed by service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumedMapsToRemove(service.getId())) {
            objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }

        // 2. remove all maps to the services consuming service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumingMapsToRemove(service.getId())) {
            objectProcessManager.scheduleProcessInstanceAsync(ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }

    protected String allocateVip(Service service) {
        if (ServiceConstants.SERVICE_LIKE.contains(service.getKind())) {
            Subnet vipSubnet = getServiceVipSubnet(service);
            String requestedVip = service.getVip();
            return allocateIpForService(service, vipSubnet, requestedVip);
        }
        return null;
    }

    protected String allocateIpForService(Object owner, Subnet subnet, String requestedIp) {
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

        vipSubnet = waitForSubnetCreation(vipSubnet);
        return vipSubnet;
    }

    public Subnet waitForSubnetCreation(Subnet subnet) {
        // wait for subnet to become active so the ip range is populated
        subnet = resourceMonitor.waitFor(subnet,
                new ResourcePredicate<Subnet>() {
                    @Override
                    public boolean evaluate(Subnet obj) {
                        return CommonStatesConstants.ACTIVE.equals(obj.getState());
                    }

                    @Override
                    public String getMessage() {
                        return "active state";
                    }
                });
        return subnet;
    }

    protected void setVIP(Service service) {
        if (!(DataAccessor.fieldBool(service, ServiceConstants.FIELD_SET_VIP) || service.getVip() != null)) {
            return;
        }
        String vip = allocateVip(service);
        if (vip != null) {
            service.setVip(vip);
            objectManager.persist(service);
        }
    }

    @SuppressWarnings("unchecked")
    protected List<PortSpec> getLaunchConfigPorts(Map<String, Object> launchConfigData) {
        if (launchConfigData.get(InstanceConstants.FIELD_PORTS) == null) {
            return new ArrayList<>();
        }
        List<String> specs = (List<String>) launchConfigData.get(InstanceConstants.FIELD_PORTS);
        List<PortSpec> ports = new ArrayList<>();
        for (String spec : specs) {
            ports.add(new PortSpec(spec));
        }
        return ports;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setPorts(Service service) {
        boolean allocatePorts = !service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE);
        Account env = objectManager.loadResource(Account.class, service.getAccountId());
        List<PooledResource> allocatedPorts = new ArrayList<>();
        if (allocatePorts) {
            allocatedPorts = allocatePorts(env, service);
        }

        // update primary launchConfig
        Map<String, Object> launchConfig = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);

        setRandomPublicPorts(env, service, launchConfig, allocatedPorts, allocatePorts);

        // update secondary launch configs
        List<Object> secondaryLaunchConfigs = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class);
        for (Object secondaryLaunchConfig : secondaryLaunchConfigs) {
            setRandomPublicPorts(env, service, (Map<String, Object>) secondaryLaunchConfig,
                    allocatedPorts,
                    allocatePorts);
        }

        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).set(launchConfig);
        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .set(secondaryLaunchConfigs);
        objectManager.persist(service);
    }

    @SuppressWarnings("unchecked")
    protected List<PooledResource> allocatePorts(Account env, Service service) {
        int toAllocate = 0;
        for (String launchConfigName : ServiceUtil.getLaunchConfigNames(service)) {
            Object ports = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                    InstanceConstants.FIELD_PORTS);
            if (ports != null) {
                for (String port : (List<String>) ports) {
                    if (new PortSpec(port).getPublicPort() == null) {
                        toAllocate++;
                    }
                }
            }
        }
        List<PooledResource> resource = null;
        PooledResourceOptions options = new PooledResourceOptions().withCount(toAllocate).withQualifier(ResourcePoolConstants.ENVIRONMENT_PORT); 
        if (toAllocate > 0) {
            resource = poolManager.allocateResource(env, service, options);
        }
        if (resource == null) {
            resource = new ArrayList<>();
        }

        if (resource.size() < toAllocate) {
            poolManager.releaseResource(env, service, options);
            throw new ResourceExhaustionException(
                    String.format("Not enough environment ports to create service. Needed: %d, available: %d", toAllocate, resource.size()), service);
        }
        return resource;
    }

    protected void setRandomPublicPorts(Account env, Service service,
            Map<String, Object> launchConfigData, List<PooledResource> allocatedPorts, boolean allocatePorts) {
        List<PortSpec> ports = getLaunchConfigPorts(launchConfigData);
        List<String> newPorts = new ArrayList<>();
        List<PortSpec> toAllocate = new ArrayList<>();
        for (PortSpec port : ports) {
            if (port.getPublicPort() == null) {
                if (!allocatePorts) {
                    if (port.getPublicPort() == null) {
                        port.setPublicPort(port.getPrivatePort());
                    }
                    newPorts.add(port.toSpec());
                } else {
                    toAllocate.add(port);
                }
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

    protected void releasePorts(Service service) {
        Account account = objectManager.loadResource(Account.class, service.getAccountId());
        poolManager.releaseResource(account, service, new PooledResourceOptions().withQualifier(
                ResourcePoolConstants.ENVIRONMENT_PORT));
    }

    protected void releaseVip(Service service) {
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
    public void removeServiceLink(final Service service, final ServiceLink serviceLink) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                ServiceConsumeMap map = consumeMapDao.findMapToRemove(service.getId(), serviceLink.getServiceId());
                if (map != null) {
                    objectProcessManager.scheduleProcessInstanceAsync(
                            ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                            map, null);
                }
            }
        });
    }

    @Override
    public boolean isSelectorLinkMatch(String selector, Service targetService) {
        if (StringUtils.isBlank(selector)) {
            return false;
        }
        Map<String, String> serviceLabels = ServiceUtil.getLaunchConfigLabels(targetService, ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        if (serviceLabels.isEmpty()) {
            return false;
        }
        return SelectorUtils.isSelectorMatch(selector, serviceLabels);

    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isSelectorContainerMatch(String selector, Instance instance) {
        if (StringUtils.isBlank(selector)) {
            return false;
        }

        Map<String, String> labels = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_LABELS).as(Map.class);
        if (labels == null || labels.isEmpty()) {
            return false;
        }
        Map<String, String> instanceLabels = new HashMap<>();
        for (Map.Entry<String, String> label : labels.entrySet()) {
            instanceLabels.put(label.getKey(), label.getValue());
        }

        return SelectorUtils.isSelectorMatch(selector, instanceLabels);
    }

    protected void updateObjectEndPoints(final Object object, final String resourceType, final Long resourceId,
            long accountId, List<PublicEndpoint> newData) {
        // have to reload the object to get the latest update for publicEndpoint
        // if don't reload, its possible that n concurrent updates would lack information.
        // update would be performed on the original object
        Object reloaded = objectManager.reload(object);
        List<PublicEndpoint> oldData = new ArrayList<>();
        oldData.addAll(DataAccessor.fields(reloaded)
                .withKey(ServiceConstants.FIELD_PUBLIC_ENDPOINTS)
                .withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, PublicEndpoint.class));

        Set<String> newPortToIp = new HashSet<>();
        Set<String> oldPortToIp = new HashSet<>();
        for (PublicEndpoint newD : newData) {
            newPortToIp.add(new StringBuilder().append(newD.getPort()).append("_").append(newD.getIpAddress())
                    .toString());
        }

        for (PublicEndpoint oldD : oldData) {
            oldPortToIp.add(new StringBuilder().append(oldD.getPort()).append("_").append(oldD.getIpAddress())
                    .toString());
        }

        if (oldPortToIp.contains(newPortToIp) && newPortToIp.contains(oldPortToIp)) {
            return;
        }

        objectManager.reload(object);
        objectManager.setFields(object, ServiceConstants.FIELD_PUBLIC_ENDPOINTS, newData);
        publishEvent(object);
    }

    protected void reconcileHostEndpointsImpl(final Host host) {
        final List<PublicEndpoint> newData = instanceDao.getPublicEndpoints(host.getAccountId(), null, host.getId());

        if (host != null && host.getRemoved() == null) {
            updateObjectEndPoints(host, host.getKind(), host.getId(),
                                    host.getAccountId(), newData);
        }
    }

    protected void reconcileServiceEndpointsImpl(final Service service) {
        final List<PublicEndpoint> newData = instanceDao.getPublicEndpoints(service.getAccountId(), service.getId(),
                null);
        if (service != null && service.getRemoved() == null && !ServiceUtil.isNoopLBService(service)) {
            updateObjectEndPoints(service, service.getKind(), service.getId(), service.getAccountId(), newData);
        }
    }

    protected void setToken(Service service) {
        String token = ApiKeyFilter.generateKeys()[1];
        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_TOKEN).set(token);
        objectManager.persist(service);
    }

    protected void removeServiceIndexes(Service service) {
        for (ServiceIndex serviceIndex : objectManager.find(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID, service.getId(),
                SERVICE_INDEX.REMOVED, null)) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, serviceIndex, null);
        }
    }

    @Override
    public void allocateIpToServiceIndex(Service service, ServiceIndex serviceIndex, String requestedIp) {
        if (StringUtils.isEmpty(serviceIndex.getAddress())) {
            String ntwkMode = networkService.getNetworkMode(DataAccessor
                    .fieldMap(service, ServiceConstants.FIELD_LAUNCH_CONFIG));
            if (ntwkMode == null) {
                return;
            }

            Network ntwk = networkService.resolveNetwork(service.getAccountId(), ntwkMode.toString());
            if (networkService.shouldAssignIpAddress(ntwk)) {
                IPAssignment assignment = networkService.assignIpAddress(ntwk, serviceIndex, requestedIp);
                if (assignment != null) {
                    setServiceIndexIp(serviceIndex, assignment.getIpAddress());
                }
            }
        }
    }

    @Override
    public void setServiceIndexIp(ServiceIndex serviceIndex, String ipAddress) {
        objectManager.setFields(serviceIndex, IpAddressConstants.FIELD_ADDRESS, ipAddress);
    }

    @Override
    public void releaseIpFromServiceIndex(Service service, ServiceIndex serviceIndex) {
        if (!StringUtils.isEmpty(serviceIndex.getAddress())) {
            String ntwkMode = networkService.getNetworkMode(DataAccessor
                    .fieldMap(service, ServiceConstants.FIELD_LAUNCH_CONFIG));
            if (ntwkMode == null) {
                return;
            }
            Network ntwk = networkService.resolveNetwork(service.getAccountId(), ntwkMode);
            networkService.releaseIpAddress(ntwk, serviceIndex);
        }
    }

    @Override
    public void updateHealthState(final Stack stack) {
        if (stack == null) {
            return;
        }
        List<Service> services = objectManager.find(Service.class, SERVICE.STACK_ID,
                stack.getId(), SERVICE.REMOVED, null);

        setServiceHealthState(services);

        setStackHealthState(stack);

        setEnvironmentHealthState(objectManager.loadResource(Account.class, stack.getAccountId()));
    }

    protected void setStackHealthState(final Stack stack) {
        String newHealthState = calculateStackHealthState(stack);
        String currentHealthState = objectManager.reload(stack).getHealthState();
        if (!newHealthState.equalsIgnoreCase(currentHealthState)) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(ServiceConstants.FIELD_HEALTH_STATE, newHealthState);
            objectManager.setFields(stack, fields);
            publishEvent(stack);
        }
    }

    protected void setEnvironmentHealthState(final Account env) {
        if (env == null) {
            return;
        }
        String newHealthState = calculateEnvironmentHealthState(env);
        String currentHealthState = objectManager.reload(env).getHealthState();
        if (!newHealthState.equalsIgnoreCase(currentHealthState)) {
            Map<String, Object> fields = new HashMap<>();
            fields.put(ServiceConstants.FIELD_HEALTH_STATE, newHealthState);
            objectManager.setFields(env, fields);
            publishEvent(env);
        }
    }

    protected String calculateStackHealthState(final Stack stack) {
        List<Service> services = objectManager.find(Service.class, SERVICE.STACK_ID, stack.getId(),
                SERVICE.REMOVED, null);
        List<HealthChecker> hcs = new ArrayList<>();
        for (Service svc : services) {
            hcs.add(new ServiceHealthCheck(svc));
        }
        return calculateHealthState(hcs);
    }

    protected String calculateEnvironmentHealthState(final Account env) {
        List<Stack> stacks = objectManager.find(Stack.class, STACK.ACCOUNT_ID, env.getId(),
                STACK.REMOVED, null);
        List<HealthChecker> hcs = new ArrayList<>();
        for (Stack stack : stacks) {
            hcs.add(new StackHealthCheck(stack));
        }
        return calculateHealthState(hcs);
    }


    private String calculateHealthState(List<? extends HealthChecker> hcs) {
        int init = 0;
        int healthy = 0;
        int expectedCount = 0;
        int startedOnce = 0;
        List<String> healthyStates = Arrays.asList(
                HealthcheckConstants.SERVICE_HEALTH_STATE_STARTED_ONCE.toLowerCase(),
                HealthcheckConstants.HEALTH_STATE_HEALTHY.toLowerCase());
        List<String> ignoreStates = Arrays.asList(CommonStatesConstants.REMOVING,
                CommonStatesConstants.REMOVED, CommonStatesConstants.PURGED, CommonStatesConstants.PURGING);
        for (HealthChecker hc : hcs) {
            String sHS = hc.getHealthState() == null ? HealthcheckConstants.HEALTH_STATE_HEALTHY : hc
                    .getHealthState().toLowerCase();
            if (ignoreStates.contains(hc.getState())) {
                continue;
            }
            expectedCount++;
            if (hc.isActive() && healthyStates.contains(sHS)) {
                if (sHS.equalsIgnoreCase(
                        HealthcheckConstants.SERVICE_HEALTH_STATE_STARTED_ONCE.toLowerCase())) {
                    startedOnce++;
                }
                healthy++;
            } else if (sHS.equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_INITIALIZING)) {
                init++;
            }
        }

        String finalHealthState = HealthcheckConstants.HEALTH_STATE_UNHEALTHY;
        if (expectedCount == 0) {
            finalHealthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
        } else if (startedOnce >= expectedCount) {
            finalHealthState = HealthcheckConstants.SERVICE_HEALTH_STATE_STARTED_ONCE;
        } else if (healthy >= expectedCount) {
            finalHealthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
        } else if (init > 0) {
            finalHealthState = HealthcheckConstants.HEALTH_STATE_INITIALIZING;
        } else if (healthy > 0) {
            finalHealthState = HealthcheckConstants.SERVICE_HEALTH_STATE_DEGRADED;
        }
        return finalHealthState;
    }

    protected interface HealthChecker{
        String getHealthState();
        boolean isActive();
        String getState();
    }

    protected class StackHealthCheck implements HealthChecker {
        Stack stack;
        protected StackHealthCheck(Stack stack) {
            this.stack = stack;
        }

        @Override
        public String getHealthState() {
            return stack.getHealthState();
        }

        @Override
        public boolean isActive() {
            List<String> activeStates = Arrays.asList(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                    CommonStatesConstants.UPDATING_ACTIVE);
            return activeStates.contains(stack.getState());
        }

        @Override
        public String getState() {
            return stack.getState();
        }
    }

    protected class ServiceHealthCheck implements HealthChecker {
        Service svc;

        protected ServiceHealthCheck(Service svc) {
            this.svc = svc;
        }

        @Override
        public String getHealthState() {
            return svc.getHealthState();
        }

        @Override
        public boolean isActive() {
            return ServiceUtil.isActiveService(svc);
        }

        @Override
        public String getState() {
            return svc.getState();
        }
    }

    protected void setServiceHealthState(final List<? extends Service> services) {
        for (Service service : services) {
            String newHealthState = calculateServiceHealthState(service);
            String currentHealthState = objectManager.reload(service).getHealthState();
            if (!newHealthState.equalsIgnoreCase(currentHealthState)) {
                Map<String, Object> fields = new HashMap<>();
                fields.put(ServiceConstants.FIELD_HEALTH_STATE, newHealthState);
                objectManager.setFields(service, fields);
                publishEvent(service);
            }
        }
    }

    protected String calculateServiceHealthState(Service service) {
        String serviceHealthState = null;
        List<String> supportedKinds = Arrays.asList(
                ServiceConstants.KIND_SERVICE.toLowerCase(),
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE.toLowerCase());
        if (!supportedKinds.contains(service.getKind().toLowerCase())) {
            serviceHealthState = HealthcheckConstants.HEALTH_STATE_HEALTHY;
        } else {
            List<? extends Instance> serviceInstances = exposeMapDao.listServiceManagedInstances(service);
            List<String> healthyStates = Arrays.asList(HealthcheckConstants.HEALTH_STATE_HEALTHY,
                    HealthcheckConstants.HEALTH_STATE_UPDATING_HEALTHY);
            List<String> initStates = Arrays.asList(HealthcheckConstants.HEALTH_STATE_INITIALIZING,
                    HealthcheckConstants.HEALTH_STATE_REINITIALIZING);

            Integer scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
            if (scale == null || ServiceUtil.isNoopService(service)) {
                scale = 0;
            }

            List<String> lcs = ServiceUtil.getLaunchConfigNames(service);
            Integer expectedScale = scale * lcs.size();
            boolean isGlobal = ServiceUtil.isGlobalService(service);
            int healthyCount = 0;
            int initCount = 0;
            int instanceCount = serviceInstances.size();
            int startedOnce = 0;
            List<String> runningStates = Arrays.asList(InstanceConstants.STATE_RUNNING);
            for (Instance instance : serviceInstances) {
                String iHS = instance.getHealthState() == null ? HealthcheckConstants.HEALTH_STATE_HEALTHY : instance
                        .getHealthState().toLowerCase();
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

            if (startedOnce > 0 && ((isGlobal && startedOnce >= instanceCount)
                    || (!isGlobal && startedOnce >= expectedScale))) {
                return HealthcheckConstants.SERVICE_HEALTH_STATE_STARTED_ONCE;
            }

            healthyCount = healthyCount + startedOnce;

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
        List<String> stoppedStates = Arrays.asList(InstanceConstants.STATE_STOPPED, InstanceConstants.STATE_STOPPING);
        if (labels.containsKey(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE)) {
            startOnce = Boolean.valueOf(((String) labels
                    .get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE)))
                    && instance.getStartCount() != null && instance.getStartCount() >= 1L
                    && stoppedStates.contains(instance.getState());
        }
        return startOnce;
    }

    protected void publishEvent(Object obj) {
        ObjectUtils.publishChanged(eventService, objectManager, obj);
    }

    @Override
    public void serviceUpdate(ConfigUpdate update) {
        if (update.getResourceId() == null) {
            return;
        }

        final Client client = new Client(Service.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(SERVICE_ENDPOINTS_UPDATE, update, client, new Runnable() {
            @Override
            public void run() {
                Service service = objectManager.loadResource(Service.class, client.getResourceId());
                if (service != null && service.getRemoved() == null) {
                    reconcileServiceEndpointsImpl(service);
                }
            }
        });
    }

    @Override
    public void hostEndpointsUpdate(ConfigUpdate update) {
        if (update.getResourceId() == null) {
            return;
        }
        final Client client = new Client(Host.class, new Long(update.getResourceId()));
        itemManager.runUpdateForEvent(HOST_ENDPOINTS_UPDATE, update, client, new Runnable() {
            @Override
            public void run() {
                Host host = objectManager.loadResource(Host.class, client.getResourceId());
                if (host != null && host.getRemoved() == null) {
                    reconcileHostEndpointsImpl(host);
                }
            }
        });
    }

    @Override
    public void reconcileServiceEndpoints(Service service) {
        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Service.class, service.getId());
        request.addItem(SERVICE_ENDPOINTS_UPDATE);
        request.withDeferredTrigger(false);
        itemManager.updateConfig(request);
    }

    @Override
    public void reconcileHostEndpoints(Host host) {
        ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Host.class, host.getId());
        request.addItem(HOST_ENDPOINTS_UPDATE);
        request.withDeferredTrigger(false);
        itemManager.updateConfig(request);
    }


    @Override
    public void registerServiceLinks(Service service) {
        registerTargetServices(service);
        registerAsTargetService(service);
    }

    private void registerAsTargetService(Service targetService) {

        List<Long> accountIds = new ArrayList<>();
        accountIds.add(targetService.getAccountId());
        List<? extends AccountLink> linkedToAccounts = objectManager.find(AccountLink.class,
                ACCOUNT_LINK.LINKED_ACCOUNT_ID, targetService.getAccountId(),
                ACCOUNT_LINK.REMOVED, null);

        // add all accounts that are linked to service's account
        for (AccountLink linkedToAccount : linkedToAccounts) {
            accountIds.add(linkedToAccount.getAccountId());
        }

        List<Service> services = new ArrayList<>();
        for (Long accountId : accountIds) {
            services.addAll(objectManager.find(Service.class, SERVICE.ACCOUNT_ID, accountId,
                    SERVICE.REMOVED, null));
        }
        for (Service service : services) {
            // skip itself
            if (targetService.getId().equals(service.getId())) {
                continue;
            }
            if (isSelectorLinkMatch(service.getSelectorLink(), targetService)) {
                addServiceLink(service, targetService);
            }
        }
    }

    private void registerTargetServices(Service service) {
        List<Long> targetAccountIds = new ArrayList<>();
        targetAccountIds.add(service.getAccountId());
        // add all accounts that are linked to service's account
        List<? extends AccountLink> linkedAccounts = objectManager.find(AccountLink.class,
                ACCOUNT_LINK.ACCOUNT_ID, service.getAccountId(),
                ACCOUNT_LINK.REMOVED, null);
        for (AccountLink linkedAccount : linkedAccounts) {
            targetAccountIds.add(linkedAccount.getLinkedAccountId());
        }

        List<Service> targetServices = new ArrayList<>();
        for (Long accountId : targetAccountIds) {
            targetServices.addAll(objectManager.find(Service.class, SERVICE.ACCOUNT_ID, accountId,
                    SERVICE.REMOVED, null));
        }

        for (Service targetService : targetServices) {
            // skip itself
            if (targetService.getId().equals(service.getId())) {
                continue;
            }
            if (isSelectorLinkMatch(service.getSelectorLink(), targetService)) {
                addServiceLink(service, targetService);
            }
        }
    }

    protected void addServiceLink(Service service, Service targetService) {
        ServiceLink link = new ServiceLink(targetService.getId(), null);
        addServiceLink(service, link);
    }

    @Override
    public void removeFromLoadBalancerServices(Service service, Instance instance) {
        if (service == null && instance == null) {
            return;
        }
        long accountId = service != null ? service.getAccountId() : instance.getAccountId();
        List<? extends Service> balancers = objectManager.find(Service.class, SERVICE.KIND,
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE, SERVICE.REMOVED, null, SERVICE.ACCOUNT_ID,
                accountId);
        for (final Service balancer : balancers) {
            LbConfig lbConfig = DataAccessor.field(balancer, ServiceConstants.FIELD_LB_CONFIG,
                    jsonMapper, LbConfig.class);
            if (lbConfig == null || lbConfig.getPortRules() == null) {
                continue;
            }
            List<PortRule> newSet = new ArrayList<>();
            boolean update = false;
            for (PortRule rule : lbConfig.getPortRules()) {
                if (rule.getInstanceId() != null && instance != null) {
                    if (rule.getInstanceId().equalsIgnoreCase(instance.getId().toString())) {
                        update = true;
                        // add a replacement rule (if present)
                        Instance replacement = objectManager.findAny(Instance.class, INSTANCE.REPLACEMENT_FOR,
                                instance.getId(), INSTANCE.REMOVED, null);
                        if (replacement == null) {
                            continue;
                        }
                        rule.setInstanceId(replacement.getId().toString());
                    }
                } else if (rule.getServiceId() != null && service != null) {
                    if (rule.getServiceId().equalsIgnoreCase(service.getId().toString())) {
                        update = true;
                        continue;
                    }
                }

                newSet.add(rule);
            }

            if (update) {
                lbConfig.setPortRules(newSet);
                final Map<String, Object> data = new HashMap<>();
                data.put(ServiceConstants.FIELD_LB_CONFIG, lbConfig);
                Service updated = lockManager.lock(new LoadBalancerServiceLock(balancer.getId()),
                        new LockCallback<Service>() {
                            @Override
                            public Service doWithLock() {
                                return objectManager.setFields(balancer, data);
                            }
                        });
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.UPDATE, updated, null);
            }
        }
    }

    @Override
    public void incrementExecutionCount(Object object) {
        objectManager.reload(object);
        Integer count = DataAccessor.fieldInteger(object, ServiceConstants.FIELD_EXECUTION_COUNT);
        if (count == null) {
            count = 0;
        }

        count = count + 1;

        Date start = DataAccessor.fieldDate(object, ServiceConstants.FIELD_EXECUTION_PERIOD_START);
        if (start == null) {
            start = new Date();
        }

        Date end = new Date(start.getTime() + EXECUTION_PERIOD.get() * 1000);
        Date now = new Date();

        try {
            if (now.after(end)) {
                count = 1;
                start = now;
            } else if (count > EXECUTION_MAX.get()) {
                throw new ProcessDelayException(new Date(System.currentTimeMillis() + EXECUTION_DELAY.get() * 1000));
            }
        } finally {
            objectManager.setFields(object,
                    ServiceConstants.FIELD_EXECUTION_COUNT, count,
                    ServiceConstants.FIELD_EXECUTION_PERIOD_START, start);
        }
    }

    @SuppressWarnings("unchecked")
    protected void createInitialServiceRevision(Service service) {
        Map<String, Object> launchConfig = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_LAUNCH_CONFIG).withDefault(Collections.EMPTY_MAP)
                .as(Map.class);
        List<Map<String, Object>> secondaryLaunchConfigs = DataAccessor.fields(service)
                .withKey(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)
                .withDefault(Collections.EMPTY_LIST).as(
                        List.class);
        InstanceRevision revision = serviceDao.createRevision(service, launchConfig, secondaryLaunchConfigs, true);
        if (service.getRevisionId() == null) {
            Map<String, Object> data = new HashMap<>();
            data.put(InstanceConstants.FIELD_REVISION_ID, revision.getId());
            objectManager.setFields(service, data);
        }
    }

    @Override
    public void resetUpgradeFlag(Service service) {
        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_IS_UPGRADE, 0);
        objectManager.setFields(objectManager.reload(service), data);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> buildDeploymentUnitInstanceData(Service service, Map<String, Object> deployParams,
            String launchConfigName) {
        Map<String, Object> serviceData = ServiceUtil.getLaunchConfigDataAsMap(service, launchConfigName);
        Map<String, Object> launchConfigItems = new HashMap<>();

        // 1. put all parameters retrieved through deployParams
        if (deployParams != null) {
            launchConfigItems.putAll(deployParams);
        }

        // 2. Get parameters defined on the service level (merge them with the ones defined in
        for (String key : serviceData.keySet()) {
            Object dataObj = serviceData.get(key);
            if (launchConfigItems.get(key) != null) {
                if (dataObj instanceof Map) {
                    // unfortunately, need to make an except for labels due to the merging aspect of the values
                    if (key.equalsIgnoreCase(InstanceConstants.FIELD_LABELS)) {
                        allocationHelper.normalizeLabels(
                                service.getStackId(),
                                (Map<String, String>) launchConfigItems.get(key),
                                (Map<String, String>) dataObj);
                        ServiceUtil.mergeLabels((Map<String, String>) launchConfigItems.get(key),
                                (Map<String, String>) dataObj);
                    } else {
                        ((Map<Object, Object>) dataObj).putAll((Map<Object, Object>) launchConfigItems.get(key));
                    }
                } else if (dataObj instanceof List) {
                    for (Object existing : (List<Object>) launchConfigItems.get(key)) {
                        if (!((List<Object>) dataObj).contains(existing)) {
                            ((List<Object>) dataObj).add(existing);
                        }
                    }
                }
            }
            if (dataObj != null) {
                launchConfigItems.put(key, dataObj);
            }
        }

        // 3. add extra parameters
        launchConfigItems.put("accountId", service.getAccountId());
        if (!launchConfigItems.containsKey(ObjectMetaDataManager.KIND_FIELD)) {
            launchConfigItems.put(ObjectMetaDataManager.KIND_FIELD, InstanceConstants.KIND_CONTAINER);
        }

        return launchConfigItems;
    }

    @Override
    public void remove(Service service) {
        upgradeMgr.finishUpgrade(service, false);
        deploymentMgr.remove(service);

        releaseVip(service);

        releasePorts(service);

        removeServiceIndexes(service);

        serviceDao.cleanupServiceRevisions(service);
    }

    @Override
    public void create(Service service) {
        setVIP(service);
        setPorts(service);
        setToken(service);
        createInitialServiceRevision(service);
    }

    @Override
    public void joinService(Instance instance) {
        if (instance.getDeploymentUnitId() != null) {
            return;
        }
        if (instance.getName() == null) {
            return;
        }

        InstanceData data = createInstanceData(instance);
        Service service = data.getService();
        Stack stack = data.getStack();
        DeploymentUnit unit = data.getUnit();
        String launchConfig = data.getLaunchConfig();
        updateInstance(instance, service, stack, unit, launchConfig);
        updateService(service);
        if (unit.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE,
                    unit, null);
        }

        if (service.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.CREATE, StandardProcess.ACTIVATE,
                    service, null);
        } else {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.UPDATE, service, null);
        }
    }

    protected void updateInstance(Instance instance, Service service, Stack stack, DeploymentUnit unit, String launchConfig) {
        exposeMapDao.createServiceInstanceMap(service, instance, true);
        ServiceIndex serviceIndex = serviceDao.createServiceIndex(service, launchConfig, "0");
        Map<String, Object> data = getDeploymentUnitInstanceData(stack, service, unit,
                launchConfig, serviceIndex, instance.getName());
        data.remove(InstanceConstants.FIELD_SERVICE_ID);
        data.put(InstanceConstants.FIELD_CONTAINER_SERVICE_ID, service.getId());
        objectManager.setFields(instance, data);
    }

    protected InstanceData createInstanceData(Instance instance) {
        DeploymentUnit du = null;
        Service service = null;
        List<Long> deps = InstanceConstants.getInstanceDependencies(instance);
        Stack stack = null;
        String launchConfig = null;
        if (!deps.isEmpty()) {
            Instance depInstance = objectManager.findAny(Instance.class, INSTANCE.ID,
                    deps.get(0));
            du = objectManager.loadResource(DeploymentUnit.class, depInstance.getDeploymentUnitId());
            service = objectManager.loadResource(Service.class, du.getServiceId());
            stack = objectManager.loadResource(Stack.class, service.getStackId());
            launchConfig = instance.getName();
        } else {
            stack = getStack(instance);
            service = createService(instance.getAccountId(), instance.getName(), stack.getId());
            du = createDeploymentUnit(instance, service);
            launchConfig = ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME;
        }

        return new InstanceData(service, stack, launchConfig, du);
    }

    public static class InstanceData {
        Service service;
        Stack stack;
        String launchConfig;
        DeploymentUnit unit;

        public InstanceData(Service service, Stack stack, String launchConfig, DeploymentUnit unit) {
            super();
            this.service = service;
            this.stack = stack;
            this.launchConfig = launchConfig;
            this.unit = unit;
        }

        public Service getService() {
            return service;
        }

        public Stack getStack() {
            return stack;
        }

        public String getLaunchConfig() {
            return launchConfig;
        }

        public DeploymentUnit getUnit() {
            return unit;
        }

        public boolean isPrimary() {
            return launchConfig.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME);
        }
    }

    public DeploymentUnit createDeploymentUnit(Instance instance, Service service) {
        DeploymentUnit du = serviceDao.createDeploymentUnit(instance.getAccountId(), service.getId(),
                service.getStackId(),
                    null, 0);
        return du;
    }

    protected Stack getStack(Instance instance) {
        Long stackId = instance.getStackId();
        if (stackId != null) {
            return objectManager.loadResource(Stack.class, stackId);
        }
        Stack stack = serviceDao.getOrCreateDefaultStack(instance.getAccountId());
        return stack;
    }

    @SuppressWarnings("unchecked")
    protected Service updateService(Service service) {
        List<? extends Instance> instances = objectManager.find(Instance.class, INSTANCE.CONTAINER_SERVICE_ID,
                service.getId(), INSTANCE.REMOVED, null);
        List<Instance> secondary = new ArrayList<>();
        Instance primary = null;
        for (Instance instance : instances) {
            Map<String, String> instanceLabels = DataAccessor.fields(instance)
                    .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
            String launchConfigName = instanceLabels
                    .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
            if (launchConfigName.equalsIgnoreCase(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
                primary = instance;
            } else {
                secondary.add(instance);
            }
        }
        Map<Long, String> containerIdToLaunchConfigName = new HashMap<>();
        containerIdToLaunchConfigName.put(primary.getId(), service.getName());
        for (Instance sec : secondary) {
            containerIdToLaunchConfigName.put(sec.getId(), sec.getName());
        }

        Map<String, Object> primaryLC = getLaunchConfig(primary, containerIdToLaunchConfigName, true);
        List<Map<String, Object>> secondaryLCs = new ArrayList<>();
        for (Instance sec : secondary) {
            secondaryLCs.add(getLaunchConfig(sec, containerIdToLaunchConfigName, false));
        }

        Map<String, Object> data = new HashMap<>();
        data.put(ServiceConstants.FIELD_LAUNCH_CONFIG, primaryLC);
        data.put(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS, secondaryLCs);
        service = objectManager.setFields(objectManager.reload(service), data);
        return service;
    }

    public Service createService(final long accountId, final String name, final long stackId) {
        return lockManager.lock(new CreateServiceLock(stackId), new LockCallback<Service>() {
            @Override
            public Service doWithLock() {
                Service service = objectManager.findAny(Service.class, SERVICE.STACK_ID, stackId, SERVICE.NAME,
                        name, SERVICE.REMOVED, null);
                if (service == null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put(ObjectMetaDataManager.NAME_FIELD, name);
                    data.put(ObjectMetaDataManager.KIND_FIELD, ServiceConstants.KIND_CONTAINER_SERVICE);
                    data.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
                    data.put(InstanceConstants.FIELD_STACK_ID, stackId);
                    data.put(ServiceConstants.FIELD_SCALE, 1);
                    data.put(ServiceConstants.FIELD_BATCHSIZE, 1);
                    data.put(ServiceConstants.FIELD_INTERVAL_MILLISEC, 2000);
                    service = objectManager.create(Service.class, data);
                }
                return service;
            }
        });
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> getLaunchConfig(Instance instance, Map<Long, String> containerToLCName, boolean isPrimary) {
        Map<String, Object> spec = instanceDao.getInstanceSpec(instance);
        if (isPrimary) {
            spec.remove(ObjectMetaDataManager.NAME_FIELD);
        } else {
            spec.put(ObjectMetaDataManager.NAME_FIELD, instance.getName());
        }
        if (spec.containsKey(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)) {
            spec.put(ServiceConstants.FIELD_NETWORK_LAUNCH_CONFIG,
                    containerToLCName.get(Long.valueOf(spec.get(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID)
                            .toString())));
            spec.remove(DockerInstanceConstants.FIELD_NETWORK_CONTAINER_ID);
        }

        Object volumesInstances = spec.get(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        if (volumesInstances != null) {
            List<String> volumesFromLaunchConfigs = new ArrayList<>();
            for (Integer instanceId : (List<Integer>) volumesInstances) {
                volumesFromLaunchConfigs.add(containerToLCName.get(instanceId.longValue()));
            }
            spec.put(ServiceConstants.FIELD_DATA_VOLUMES_LAUNCH_CONFIG, volumesFromLaunchConfigs);
            spec.remove(DockerInstanceConstants.FIELD_VOLUMES_FROM);
        }

        return spec;
    }

    protected void setStack(Instance instance, Map<Object, Object> data) {
        Long stackId = instance.getStackId();
        if (stackId != null) {
            return;
        }
        Stack stack = serviceDao.getOrCreateDefaultStack(instance.getAccountId());
        data.put(InstanceConstants.FIELD_STACK_ID, stack.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDeploymentUnitInstanceData(Stack stack, Service service, DeploymentUnit unit,
            String launchConfigName, ServiceIndex serviceIndex, String instanceName) {
        Map<String, Object> deploymentUnitParams = getDeploymentUnitData(stack, service, unit,
                launchConfigName, serviceIndex);
        Map<String, Object> deploymentUnitInstanceData = buildDeploymentUnitInstanceData(service,
                deploymentUnitParams, launchConfigName);
        removeNamedVolumesFromLaunchConfig(deploymentUnitParams,
                deploymentUnitInstanceData);
        deploymentUnitInstanceData.put("name", instanceName);
        Object labels = deploymentUnitInstanceData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            String overrideHostName = ((Map<String, String>) labels)
                    .get(ServiceConstants.LABEL_OVERRIDE_HOSTNAME);
            if (StringUtils.equalsIgnoreCase(overrideHostName, "container_name")) {
                String domainName = (String) deploymentUnitInstanceData.get(DockerInstanceConstants.FIELD_DOMAIN_NAME);
                String overrideName = getOverrideHostName(domainName, instanceName);
                deploymentUnitInstanceData.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
            }
        }

        return deploymentUnitInstanceData;
    }

    private static String getOverrideHostName(String domainName, String instanceName) {
        String overrideName = instanceName;
        if (instanceName != null && instanceName.length() > 64) {
            // legacy code - to support old data where service suffix object wasn't created
            String serviceSuffix = ServiceConstants.getServiceIndexFromInstanceName(instanceName);
            String serviceSuffixWDivider = instanceName.substring(instanceName.lastIndexOf(serviceSuffix) - 1);
            int truncateIndex = 64 - serviceSuffixWDivider.length();
            if (domainName != null) {
                truncateIndex = truncateIndex - domainName.length() - 1;
            }
            overrideName = instanceName.substring(0, truncateIndex) + serviceSuffixWDivider;
        }
        return overrideName;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getDeploymentUnitData(Stack stack, Service service, DeploymentUnit unit,
            String launchConfigName, ServiceIndex serviceIndex) {
        Map<String, Object> deployParams = new HashMap<>();
        Map<String, String> instanceLabels = generateDeploymentUnitLabels(service, stack, unit.getUuid(),
                launchConfigName);
        instanceLabels.putAll(DataAccessor.fields(unit).withKey(InstanceConstants.FIELD_LABELS)
                .withDefault(Collections.EMPTY_MAP).as(Map.class));
        deployParams.put(InstanceConstants.FIELD_LABELS, instanceLabels);

        Object hostId = instanceLabels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        if (hostId != null) {
            deployParams.put(InstanceConstants.FIELD_REQUESTED_HOST_ID, hostId);
        }

        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_UUID, unit.getUuid());
        deployParams.put(InstanceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        deployParams.put(ServiceConstants.FIELD_VERSION, ServiceUtil.getLaunchConfigObject(
                service, launchConfigName, ServiceConstants.FIELD_VERSION));
        addDns(stack, service, launchConfigName, deployParams);
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_CONTAINER_SERVICE)) {
            deployParams.put(InstanceConstants.FIELD_CONTAINER_SERVICE_ID, service.getId());
        } else {
            deployParams.put(InstanceConstants.FIELD_SERVICE_ID, service.getId());
        }

        deployParams.put(InstanceConstants.FIELD_STACK_ID, service.getStackId());
        if (serviceIndex != null) {
            deployParams.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID,
                    serviceIndex.getId());
            deployParams.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                    serviceIndex.getServiceIndex());
            deployParams.put(InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS, serviceIndex.getAddress());
        }

        List<String> namedVolumes = getNamedVolumes(service, launchConfigName);
        List<String> internalVolumes = new ArrayList<>();
        Map<String, Long> volumeMounts = new HashMap<>();
        Iterator<String> it = namedVolumes.iterator();
        while (it.hasNext()) {
            String namedVolume = it.next();
            getOrCreateVolume(stack, service, unit, namedVolume, volumeMounts, internalVolumes);
            if (!internalVolumes.contains(namedVolume)) {
                it.remove();
            }
        }

        deployParams.put(ServiceConstants.FIELD_INTERNAL_VOLUMES, namedVolumes);
        deployParams.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, volumeMounts);
        return deployParams;
    }

    protected Map<String, String> generateDeploymentUnitLabels(Service service, Stack stack,
            String deploymentUnitUUID,
            String launchConfigName) {
        Map<String, String> labels = new HashMap<>();
        String serviceName = service.getName();
        if (!ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfigName)) {
            serviceName = serviceName + '/' + launchConfigName;
        }
        String envName = stack.getName();
        labels.put(ServiceConstants.LABEL_STACK_NAME, envName);
        labels.put(ServiceConstants.LABEL_STACK_SERVICE_NAME, envName + "/" + serviceName);

        // LEGACY: keeping backwards compatibility with 'project'
        labels.put(ServiceConstants.LABEL_PROJECT_NAME, envName);
        labels.put(ServiceConstants.LABEL_PROJECT_SERVICE_NAME, envName + "/" + serviceName);

        /*
         * Put label 'io.rancher.deployment.unit=this.uuid' on each one. This way
         * we can reference a set of containers later.
         */
        labels.put(ServiceConstants.LABEL_SERVICE_DEPLOYMENT_UNIT, deploymentUnitUUID);

        /*
         * 
         * Put label with launch config name
         */
        labels.put(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG, launchConfigName);

        return labels;
    }

    protected void getOrCreateVolume(Stack stack, Service service, DeploymentUnit unit, String volumeName,
            Map<String, Long> volumeMounts,
            List<String> internalVolumes) {
        if (unit.getServiceIndex() == null) {
            return;
        }
        String[] splitted = volumeName.split(":");
        String volumeNamePostfix = splitted[0];
        String volumePath = volumeName.replaceFirst(splitted[0] + ":", "");

        final VolumeTemplate template = objectManager.findOne(VolumeTemplate.class, VOLUME_TEMPLATE.ACCOUNT_ID,
                service.getAccountId(), VOLUME_TEMPLATE.REMOVED, null, VOLUME_TEMPLATE.NAME, splitted[0],
                VOLUME_TEMPLATE.STACK_ID, stack.getId());
        if (template == null) {
            return;
        }

        Volume volume = null;
        if (template.getExternal()) {
            // external volume should exist, otherwise fail
            volume = objectManager.findAny(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                    VOLUME.REMOVED, null, VOLUME.NAME, template.getName());
            if (volume == null) {
                throw new DeploymentUnitAllocateException("Failed to locate volume for for deployment unit ["
                        + unit.getUuid() + "]", null, unit);
            }
            return;
        } else {
            final String postfix = io.cattle.platform.util.resource.UUID.randomUUID().toString();
            if (template.getPerContainer()) {
                String name = stack.getName() + "_" + volumeNamePostfix + "_" + unit.getServiceIndex() + "_"
                        + unit.getUuid() + "_";
                // append 5 random chars
                List<? extends Volume> volumes = objectManager
                        .find(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(), VOLUME.STACK_ID,
                                stack.getId(), VOLUME.DEPLOYMENT_UNIT_ID, unit.getId());
                for (Volume vol : volumes) {
                    if (vol.getName().startsWith(name)) {
                        volume = vol;
                        break;
                    }
                }
                if (volume == null) {
                    volume = createVolume(service, unit, template, name + postfix.substring(0, 5));
                }
            } else {
                final String name = stack.getName() + "_" + volumeNamePostfix + "_";
                volume = lockManager.lock(new StackVolumeLock(stack, name), new LockCallback<Volume>() {
                    @Override
                    public Volume doWithLock() {
                        Volume existing = null;
                        List<? extends Volume> volumes = objectManager
                                .find(Volume.class, VOLUME.ACCOUNT_ID, service.getAccountId(),
                                        VOLUME.REMOVED, null, VOLUME.VOLUME_TEMPLATE_ID, template.getId(),
                                        VOLUME.STACK_ID,
                                        stack.getId());
                        for (Volume vol : volumes) {
                            if (vol.getName().startsWith(name)) {
                                existing = vol;
                                break;
                            }
                        }
                        if (existing != null) {
                            return existing;
                        }
                        return createVolume(service, unit, template, new String(name + postfix.substring(0, 5)));
                    }
                });
            }
        }
        volumeMounts.put(volumePath, volume.getId());
        internalVolumes.add(volumeName);
    }

    private Volume createVolume(Service service, DeploymentUnit unit, VolumeTemplate template, String name) {
        Map<String, Object> params = new HashMap<>();
        if (template.getPerContainer()) {
            params.put(ServiceConstants.FIELD_DEPLOYMENT_UNIT_ID, unit.getId());
        }
        params.put("name", name);
        params.put("accountId", service.getAccountId());
        params.put(InstanceConstants.FIELD_STACK_ID, service.getStackId());
        params.put(ServiceConstants.FIELD_VOLUME_TEMPLATE_ID, template.getId());
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER_OPTS,
                DataAccessor.fieldMap(template, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS));
        params.put(VolumeConstants.FIELD_VOLUME_DRIVER, template.getDriver());
        return resourceDao.createAndSchedule(Volume.class, params);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getNamedVolumes(Service service, String launchConfigName) {
        Object dataVolumesObj = ServiceUtil.getLaunchConfigObject(service, launchConfigName,
                InstanceConstants.FIELD_DATA_VOLUMES);
        List<String> namedVolumes = new ArrayList<>();
        if (dataVolumesObj != null) {
            for (String volume : (List<String>) dataVolumesObj) {
                if (isNamedVolume(volume)) {
                    namedVolumes.add(volume);
                }
            }
        }
        return namedVolumes;
    }

    protected boolean isNamedVolume(String volumeName) {
        String[] splitted = volumeName.split(":");
        if (splitted.length < 2) {
            return false;
        }
        if (splitted[0].contains("/")) {
            return false;
        }
        return true;
    }

    protected void addDns(Stack stack, Service service, String launchConfigName, Map<String, Object> deployParams) {
        boolean addDns = true;
        Object labelsObj = ServiceUtil.getLaunchConfigObject(
                service, launchConfigName, InstanceConstants.FIELD_LABELS);
        if (labelsObj != null) {
            Map<String, Object> labels = CollectionUtils.toMap(labelsObj);
            if (labels.containsKey(SystemLabels.LABEL_USE_RANCHER_DNS)
                    && !Boolean.valueOf(SystemLabels.LABEL_USE_RANCHER_DNS))
                addDns = false;
        }

        if (addDns) {
            deployParams.put(DockerInstanceConstants.FIELD_DNS_SEARCH, getSearchDomains(stack, service));
        }
    }

    protected List<String> getSearchDomains(Stack stack, Service service) {
        String stackNamespace = ServiceUtil.getStackNamespace(stack, service);
        String serviceNamespace = ServiceUtil
                .getServiceNamespace(stack, service);
        return Arrays.asList(stackNamespace, serviceNamespace);
    }

    @SuppressWarnings("unchecked")
    private void removeNamedVolumesFromLaunchConfig(Map<String, Object> deploymentUnitParams,
            Map<String, Object> deploymentData) {
        // remove named volumes from the list
        if (deploymentData.get(InstanceConstants.FIELD_DATA_VOLUMES) != null) {
            List<String> dataVolumes = new ArrayList<>();
            dataVolumes.addAll((List<String>) deploymentData.get(InstanceConstants.FIELD_DATA_VOLUMES));
            if (deploymentUnitParams.get(ServiceConstants.FIELD_INTERNAL_VOLUMES) != null) {
                for (String namedVolume : (List<String>) deploymentUnitParams
                        .get(ServiceConstants.FIELD_INTERNAL_VOLUMES)) {
                    dataVolumes.remove(namedVolume);
                }
                deploymentData.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
            }
            deploymentData.remove(ServiceConstants.FIELD_INTERNAL_VOLUMES);
        }
    }

    @Override
    public void leaveService(Instance instance) {
        if (instance.getDeploymentUnitId() == null) {
            return;
        }
        if (serviceDao.isServiceManagedInstance(instance)) {
            return;
        }
        if (isPrimaryLaunchConfig(instance)) {
            Service service = objectManager.loadResource(Service.class, instance.getContainerServiceId());
            if (!service.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, service, null);
            }
        }
    }

    @SuppressWarnings("unchecked")
    boolean isPrimaryLaunchConfig(Instance instance) {
        Map<String, String> instanceLabels = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
        String launchConfigName = instanceLabels
                .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
        return ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equals(launchConfigName);
    }
}
