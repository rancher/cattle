package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.revision.RevisionManager;

public class ServiceLifecycleManagerImpl implements ServiceLifecycleManager {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ResourcePoolManager poolManager;
    NetworkService networkService;
    ServiceDao serviceDao;
    RevisionManager revisionManager;
    LoadBalancerService loadbalancerService;

    public ServiceLifecycleManagerImpl(ObjectManager objectManager, ResourcePoolManager poolManager,
            NetworkService networkService, ServiceDao serviceDao, RevisionManager revisionManager,
            LoadBalancerService loadbalancerService, ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.poolManager = poolManager;
        this.networkService = networkService;
        this.serviceDao = serviceDao;
        this.revisionManager = revisionManager;
        this.loadbalancerService = loadbalancerService;
        this.processManager = processManager;
    }

    @Override
    public void preStart(Instance instance) {
        InstanceHealthCheck hc = DataAccessor.field(instance, InstanceConstants.FIELD_HEALTH_CHECK, InstanceHealthCheck.class);
        if (hc == null) {
            instance.setHealthState(null);
        } else {
            instance.setHealthState(HealthcheckConstants.HEALTH_STATE_INITIALIZING);
        }
    }

    @Override
    public void postRemove(Instance instance) {
        loadbalancerService.removeFromLoadBalancerServices(instance);
        revisionManager.leaveDeploymentUnit(instance);
    }

    protected void releasePorts(Service service) {
        Account account = objectManager.loadResource(Account.class, service.getAccountId());
        poolManager.releaseResource(account, service, new PooledResourceOptions().withQualifier(
                ResourcePoolConstants.ENVIRONMENT_PORT));
    }

    protected void setToken(Service service) {
        String token = SecurityConstants.generateKeys()[1];
        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_TOKEN).set(token);
    }

    @Override
    public void remove(Service service) {
        loadbalancerService.removeFromLoadBalancerServices(service);

        releasePorts(service);
    }

    @Override
    public void create(Service service) {
        setToken(service);

        objectManager.persist(service);
    }

}
