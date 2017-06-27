package io.cattle.platform.lifecycle.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

public class ServiceLifecycleManagerImpl implements ServiceLifecycleManager {

    ServiceConsumeMapDao consumeMapDao;
    ObjectManager objectManager;
    NetworkDao ntwkDao;
    ObjectProcessManager objectProcessManager;
    ServiceExposeMapDao exposeMapDao;
    ResourcePoolManager poolManager;
    ResourceMonitor resourceMonitor;
    EventService eventService;
    NetworkService networkService;
    ServiceDao serviceDao;
    RevisionManager revisionManager;
    ObjectProcessManager processManager;
    LoadBalancerService loadbalancerService;

    public ServiceLifecycleManagerImpl(ServiceConsumeMapDao consumeMapDao, ObjectManager objectManager, NetworkDao ntwkDao,
            ObjectProcessManager objectProcessManager, ServiceExposeMapDao exposeMapDao, ResourcePoolManager poolManager, ResourceMonitor resourceMonitor,
            EventService eventService, NetworkService networkService, ServiceDao serviceDao, RevisionManager revisionManager,
            ObjectProcessManager processManager, LoadBalancerService loadbalancerService) {
        super();
        this.consumeMapDao = consumeMapDao;
        this.objectManager = objectManager;
        this.ntwkDao = ntwkDao;
        this.objectProcessManager = objectProcessManager;
        this.exposeMapDao = exposeMapDao;
        this.poolManager = poolManager;
        this.resourceMonitor = resourceMonitor;
        this.eventService = eventService;
        this.networkService = networkService;
        this.serviceDao = serviceDao;
        this.revisionManager = revisionManager;
        this.processManager = processManager;
        this.loadbalancerService = loadbalancerService;
    }

    @Override
    public void preRemove(Instance instance) {
        exposeMapDao.deleteServiceExposeMaps(instance);
    }

    @Override
    public void postRemove(Instance instance) {
        cleanupHealthcheckMaps(instance);
        loadbalancerService.removeFromLoadBalancerServices(instance);
        revisionManager.leaveDeploymentUnit(instance);
    }

    private void cleanupHealthcheckMaps(Instance instance) {
        HealthcheckInstance hi = objectManager.findAny(HealthcheckInstance.class, HEALTHCHECK_INSTANCE.INSTANCE_ID,
                instance.getId(),
                HEALTHCHECK_INSTANCE.REMOVED, null);

        if (hi == null) {
            return;
        }

        List<? extends HealthcheckInstanceHostMap> hostMaps = objectManager.find(HealthcheckInstanceHostMap.class,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTHCHECK_INSTANCE_ID, hi.getId(),
                HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED, null);

        for (HealthcheckInstanceHostMap hostMap : hostMaps) {
            processManager.remove(hostMap, null);
        }

        processManager.remove(hi, null);
    }

    private void removeServiceLinks(Service service) {
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
    public void updateHealthState(Long stackId) {
        Stack stack = objectManager.loadResource(Stack.class, stackId);
        if (stack == null) {
            return;
        }

        List<Service> services = objectManager.find(Service.class, SERVICE.STACK_ID,
                stackId, SERVICE.REMOVED, null);

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
                CommonStatesConstants.REMOVED);
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
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE.toLowerCase(),
                ServiceConstants.KIND_SCALING_GROUP_SERVICE.toLowerCase());
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
    public void remove(Service service) {
        List<? extends ServiceExposeMap> unmanagedMaps = exposeMapDao
                .getUnmanagedServiceInstanceMapsToRemove(service.getId());

        for (ServiceExposeMap unmanagedMap : unmanagedMaps) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unmanagedMap, null);
        }

        removeServiceLinks(service);
        loadbalancerService.removeFromLoadBalancerServices(service);

        releaseVip(service);

        releasePorts(service);

        removeServiceIndexes(service);

        serviceDao.cleanupRevisions(service);
    }

    @Override
    public void create(Service service) {
        setVIP(service);
        setToken(service);
    }

}
