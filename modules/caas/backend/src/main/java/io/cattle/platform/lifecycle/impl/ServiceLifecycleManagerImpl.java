package io.cattle.platform.lifecycle.impl;


import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.addon.Link;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.loadbalancer.LoadBalancerService;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.lock.InstanceVolumeAccessModeLock;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.revision.RevisionManager;
import io.cattle.platform.lock.LockManager;
import org.h2.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceLifecycleManagerImpl implements ServiceLifecycleManager {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    ResourcePoolManager poolManager;
    NetworkService networkService;
    ServiceDao serviceDao;
    RevisionManager revisionManager;
    LoadBalancerService loadbalancerService;
    LockManager lockManager;

    public ServiceLifecycleManagerImpl(ObjectManager objectManager, ResourcePoolManager poolManager,
                                       NetworkService networkService, ServiceDao serviceDao, RevisionManager revisionManager,
                                       LoadBalancerService loadbalancerService, ObjectProcessManager processManager,
                                       LockManager lockManager) {
        super();
        this.objectManager = objectManager;
        this.poolManager = poolManager;
        this.networkService = networkService;
        this.serviceDao = serviceDao;
        this.revisionManager = revisionManager;
        this.loadbalancerService = loadbalancerService;
        this.processManager = processManager;
        this.lockManager = lockManager;
    }

    @Override
    public void preStart(Instance instance) {
        InstanceHealthCheck hc = DataAccessor.field(instance, InstanceConstants.FIELD_HEALTH_CHECK, InstanceHealthCheck.class);
        DataAccessor.setField(instance, InstanceConstants.FIELD_HEALTHCHECK_STATES, null);
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
        String token = CredentialConstants.generateKeys()[1];
        DataAccessor.fields(service).withKey(ServiceConstants.FIELD_TOKEN).set(token);
    }

    @Override
    public void remove(Service service) {
        loadbalancerService.removeFromLoadBalancerServices(service);
        removeFromAliasService(service);
        releasePorts(service);
    }


    protected void removeFromAliasService(Service service) {
        List<? extends Service> aliasServices = objectManager.find(Service.class,
                SERVICE.KIND, ServiceConstants.KIND_DNS_SERVICE,
                SERVICE.REMOVED, null,
                SERVICE.ACCOUNT_ID, service.getAccountId());
        Map<Long, Stack> stacks = new HashMap<>();
        Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
        stacks.put(service.getStackId(), stack);
        for (Service aliasSvc : aliasServices) {
            lockManager.lock(new AliasServiceLinkLock(aliasSvc.getId()), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    List<Link> links = DataAccessor.fieldObjectList(aliasSvc,
                            ServiceConstants.FIELD_SERVICE_LINKS, Link.class);
                    if (links.isEmpty()) {
                        return;
                    }
                    Stack aliasStack = stacks.get(aliasSvc.getStackId());
                    if (aliasStack == null) {
                        aliasStack = objectManager.loadResource(Stack.class, aliasSvc.getStackId());
                        stacks.put(aliasStack.getId(), aliasStack);
                    }

                    boolean changed = false;
                    List<Link> newLinks = new ArrayList<>();
                    for (Link link : links) {
                        String linkName = link.getName();
                        String[] splitted = linkName.split("/");
                        boolean remove = false;
                        if (splitted.length == 1) {
                            remove = StringUtils.equals(splitted[0], service.getName())
                                    && StringUtils.equals(aliasStack.getName(), stack.getName());
                        } else if (splitted.length == 2) {
                            remove = StringUtils.equals(splitted[1], service.getName())
                                    && StringUtils.equals(splitted[0], stack.getName());
                        }
                        if (remove) {
                            changed = true;
                        } else {
                            newLinks.add(link);
                        }
                    }

                    if (changed) {
                        objectManager.setFields(aliasSvc, ServiceConstants.FIELD_SERVICE_LINKS, newLinks);
                    }
                }
                });
        }
    }

    @Override
    public void create(Service service) {
        setToken(service);

        objectManager.persist(service);
    }

}
