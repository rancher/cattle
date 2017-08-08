package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.iaas.api.auth.SecurityConstants;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.revision.RevisionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceLifecycleManagerImpl implements ServiceLifecycleManager {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ResourcePoolManager poolManager;
    NetworkService networkService;
    ServiceDao serviceDao;
    RevisionManager revisionManager;

    public ServiceLifecycleManagerImpl(ObjectManager objectManager, ResourcePoolManager poolManager,
            NetworkService networkService, ServiceDao serviceDao, RevisionManager revisionManager,
            ObjectProcessManager processManager) {
        super();
        this.objectManager = objectManager;
        this.poolManager = poolManager;
        this.networkService = networkService;
        this.serviceDao = serviceDao;
        this.revisionManager = revisionManager;
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
        revisionManager.leaveDeploymentUnit(instance);
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

    protected void setToken(Service service) {
        String token = SecurityConstants.generateKeys()[1];
        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_TOKEN).set(token);
    }

    @Override
    public void remove(Service service) {
        releasePorts(service);
    }

    @Override
    public void create(Service service) {
        setToken(service);

        objectManager.persist(service);
    }

}
