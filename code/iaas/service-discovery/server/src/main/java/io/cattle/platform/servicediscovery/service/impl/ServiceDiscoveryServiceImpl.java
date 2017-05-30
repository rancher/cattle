package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.BackoffTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.events.ConfigUpdate;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Backoff;
import io.cattle.platform.core.model.Host;
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
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockManager;
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
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.servicediscovery.deployment.impl.lock.LoadBalancerServiceLock;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicLongProperty;

public class ServiceDiscoveryServiceImpl implements ServiceDiscoveryService {

    private static final String HOST_ENDPOINTS_UPDATE = "host-endpoints-update";
    private static final String SERVICE_ENDPOINTS_UPDATE = "service-endpoints-update";

    private static final DynamicLongProperty EXECUTION_TOKEN_INTERVAL = ArchaiusUtil.getLong("execution.token.every.millis");
    private static final DynamicLongProperty EXECUTION_TOKENS_MAX = ArchaiusUtil.getLong("execution.tokens.max");

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
    EventService eventService;
    @Inject
    InstanceDao instanceDao;
    @Inject
    ConfigItemStatusManager itemManager;
    @Inject
    NetworkService networkService;
    @Inject
    ServiceDao serviceDao;

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

    protected void updateObjectEndPoints(final Object object, List<PublicEndpoint> newData) {
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
            updateObjectEndPoints(host, newData);
        }
    }

    protected void reconcileServiceEndpointsImpl(final Service service) {
        final List<PublicEndpoint> newData = instanceDao.getPublicEndpoints(service.getAccountId(), service.getId(),
                null);
        if (service != null && service.getRemoved() == null && !ServiceUtil.isNoopLBService(service)) {
            updateObjectEndPoints(service, newData);
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
    public void removeFromLoadBalancerServices(Service service) {
        String serviceId = service.getId().toString();
        removeFromLoadBalancer(service.getAccountId(), (rule, balancer) -> {
            if (serviceId.equals(rule.getServiceId())) {
                return null;
            }
            return rule;
        });
    }

    @Override
    public void removeFromLoadBalancerServices(Instance instance) {
        String instanceId = instance.getId().toString();
        removeFromLoadBalancer(instance.getAccountId(), (rule, balancer) -> {
            if (instanceId.equals(rule.getInstanceId())) {
                if (instance.getServiceId() == null) {
                    Map<String, Object> portRules = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LB_RULES_ON_REMOVE);
                    PortRule newRule = new PortRule(rule);
                    newRule.setInstanceId(null);

                    Object list = portRules.get(balancer.getId().toString());
                    if (list == null) {
                        list = Arrays.asList(newRule);
                    } else {
                        List<PortRule> ruleList = jsonMapper.convertCollectionValue(list, List.class, PortRule.class);
                        if (!ruleList.contains(newRule)) {
                            ruleList.add(newRule);
                        }
                        list = ruleList;
                    }
                    portRules.put(balancer.getId().toString(), list);
                    objectManager.setFields(instance,
                            InstanceConstants.FIELD_LB_RULES_ON_REMOVE, portRules);
                }
                return null;
            }
            return rule;
        });
    }

    protected void removeFromLoadBalancer(long accountId, BiFunction<PortRule, Service, PortRule> fun) {
        List<? extends Service> balancers = objectManager.find(Service.class,
                SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE,
                SERVICE.REMOVED, null,
                SERVICE.ACCOUNT_ID, accountId);
        for (Service i : balancers) {
            lockManager.lock(new LoadBalancerServiceLock(i.getId()), () -> {
                Service balancer = objectManager.loadResource(Service.class, i.getId());
                LbConfig lbConfig = DataAccessor.field(balancer, ServiceConstants.FIELD_LB_CONFIG,
                        jsonMapper, LbConfig.class);
                if (lbConfig.getPortRules() == null) {
                    return null;
                }

                boolean changed = false;
                List<PortRule> newRules = new ArrayList<>();
                for (PortRule rule : lbConfig.getPortRules()) {
                    PortRule newRule = fun.apply(rule, balancer);
                    if (newRule == null) {
                        changed = true;
                    } else if (newRule != rule) {
                        newRules.add(newRule);
                        changed = true;
                    } else {
                        newRules.add(newRule);
                    }
                }

                if (changed) {
                    lbConfig.setPortRules(newRules);
                    objectManager.setFields(balancer, ServiceConstants.FIELD_LB_CONFIG, lbConfig);
                }

                return null;
            });
        }
    }

    @Override
    public void incrementExecutionCount(String type, Long id) {
        Backoff backoff = objectManager.findAny(Backoff.class,
                BACKOFF.RESOURCE_TYPE, type,
                BACKOFF.RESOURCE_ID, id);

        if (backoff == null) {
            backoff = objectManager.newRecord(Backoff.class);
            backoff.setPeriod(0L);
            backoff.setCount(0L);
            backoff.setResourceType(type);
            backoff.setResourceId(id);
        }

        Date lastRun = new Date(backoff.getPeriod());
        Date now = new Date();
        long tokens = backoff.getCount();

        tokens += (now.getTime() - lastRun.getTime())/EXECUTION_TOKEN_INTERVAL.get();

        if (tokens > EXECUTION_TOKENS_MAX.get()) {
            tokens = EXECUTION_TOKENS_MAX.get();
        }

        if (tokens == 0) {
            throw new ProcessDelayException(null);
        }

        tokens--;

        backoff.setPeriod(now.getTime());
        backoff.setCount(tokens);
        if (backoff.getId() == null) {
            objectManager.create(backoff);
        } else {
            objectManager.persist(backoff);
        }
    }

    @Override
    public void remove(Service service) {
        List<? extends ServiceExposeMap> unmanagedMaps = exposeMapDao
                .getUnmanagedServiceInstanceMapsToRemove(service.getId());

        for (ServiceExposeMap unmanagedMap : unmanagedMaps) {
            objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, unmanagedMap, null);
        }

        removeServiceLinks(service);
        removeFromLoadBalancerServices(service);

        releaseVip(service);

        releasePorts(service);

        removeServiceIndexes(service);

        // TODO: ADD BACK
        //serviceDao.cleanupRevisions(service);
    }

    @Override
    public void create(Service service) {
        setVIP(service);
        setToken(service);
    }

    @Override
    public void addToBalancerService(Long serviceId, List<PortRule> rules) {
        lockManager.lock(new LoadBalancerServiceLock(serviceId), () -> {
            Service balancer = objectManager.loadResource(Service.class, serviceId);
            LbConfig lbConfig = DataAccessor.field(balancer, ServiceConstants.FIELD_LB_CONFIG,
                        jsonMapper, LbConfig.class);
            List<PortRule> newRules = new ArrayList<>();
            if (lbConfig.getPortRules() != null) {
                newRules.addAll(lbConfig.getPortRules());
            }
            boolean changed = false;
            for (PortRule newRule : rules) {
                if (!newRules.contains(newRule)) {
                    newRules.add(newRule);
                    changed = true;
                }
            }
            if (changed) {
                lbConfig.setPortRules(newRules);
                objectManager.setFields(balancer, ServiceConstants.FIELD_LB_CONFIG, lbConfig);
            }
            return null;
        });
    }

}
