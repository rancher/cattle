package io.cattle.platform.servicediscovery.service.impl;

import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.SubnetTable.SUBNET;
import io.cattle.platform.allocator.service.AllocatorService;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.SubnetConstants;
import io.cattle.platform.core.dao.LabelsDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Label;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.framework.event.util.EventUtils;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.process.lock.HostEndpointsUpdateLock;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants.KIND;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.api.util.selector.SelectorUtils;
import io.cattle.platform.servicediscovery.deployment.impl.ServiceEndpointsUpdateLock;
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
}
