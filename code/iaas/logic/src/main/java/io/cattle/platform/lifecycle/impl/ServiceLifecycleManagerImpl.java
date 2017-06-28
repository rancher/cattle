package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.iaas.api.filter.apikey.ApiKeyFilter;
import io.cattle.platform.iaas.api.service.RevisionManager;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class ServiceLifecycleManagerImpl implements ServiceLifecycleManager {

    ServiceConsumeMapDao consumeMapDao;
    ObjectManager objectManager;
    ServiceExposeMapDao exposeMapDao;
    ResourcePoolManager poolManager;
    NetworkService networkService;
    ServiceDao serviceDao;
    RevisionManager revisionManager;
    LoadBalancerService loadbalancerService;

    public ServiceLifecycleManagerImpl(ServiceConsumeMapDao consumeMapDao, ObjectManager objectManager,
            ServiceExposeMapDao exposeMapDao, ResourcePoolManager poolManager,
            NetworkService networkService, ServiceDao serviceDao, RevisionManager revisionManager,
            LoadBalancerService loadbalancerService) {
        super();
        this.consumeMapDao = consumeMapDao;
        this.objectManager = objectManager;
        this.exposeMapDao = exposeMapDao;
        this.poolManager = poolManager;
        this.networkService = networkService;
        this.serviceDao = serviceDao;
        this.revisionManager = revisionManager;
        this.loadbalancerService = loadbalancerService;
    }

    @Override
    public void preRemove(Instance instance) {
        exposeMapDao.deleteServiceExposeMaps(instance);
    }

    @Override
    public void postRemove(Instance instance) {
        loadbalancerService.removeFromLoadBalancerServices(instance);
        revisionManager.leaveDeploymentUnit(instance);
    }

    private void removeServiceLinks(Service service) {
        // 1. remove all maps to the services consumed by service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumedMapsToRemove(service.getId())) {
            objectManager.delete(map);
        }

        // 2. remove all maps to the services consuming service specified
        for (ServiceConsumeMap map : consumeMapDao.findConsumingMapsToRemove(service.getId())) {
            objectManager.delete(map);
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
    public void remove(Service service) {
        List<? extends ServiceExposeMap> unmanagedMaps = exposeMapDao
                .getUnmanagedServiceInstanceMapsToRemove(service.getId());

        for (ServiceExposeMap unmanagedMap : unmanagedMaps) {
            objectManager.delete(unmanagedMap);
        }

        removeServiceLinks(service);
        loadbalancerService.removeFromLoadBalancerServices(service);

        releasePorts(service);

        serviceDao.cleanupRevisions(service);
    }

    @Override
    public void create(Service service) {
        setToken(service);

        objectManager.persist(service);
    }

}
