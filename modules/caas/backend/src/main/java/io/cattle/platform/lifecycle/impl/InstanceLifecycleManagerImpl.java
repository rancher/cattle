package io.cattle.platform.lifecycle.impl;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.backpopulate.BackPopulater;
import io.cattle.platform.core.addon.LogConfig;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.AgentLifecycleManager;
import io.cattle.platform.lifecycle.AllocationLifecycleManager;
import io.cattle.platform.lifecycle.InstanceLifecycleManager;
import io.cattle.platform.lifecycle.K8sLifecycleManager;
import io.cattle.platform.lifecycle.NetworkLifecycleManager;
import io.cattle.platform.lifecycle.RestartLifecycleManager;
import io.cattle.platform.lifecycle.SecretsLifecycleManager;
import io.cattle.platform.lifecycle.ServiceLifecycleManager;
import io.cattle.platform.lifecycle.VirtualMachineLifecycleManager;
import io.cattle.platform.lifecycle.VolumeLifecycleManager;
import io.cattle.platform.lifecycle.util.LifecycleException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.storage.ImageCredentialLookup;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

import static io.cattle.platform.object.util.DataAccessor.*;

public class InstanceLifecycleManagerImpl implements InstanceLifecycleManager {

    K8sLifecycleManager k8sLifecycle;
    VirtualMachineLifecycleManager vmLifecycle;
    VolumeLifecycleManager volumeLifecycle;
    ObjectManager objectManager;
    ImageCredentialLookup credLookup;
    ServiceDao svcDao;
    TransactionDelegate transaction;
    NetworkLifecycleManager networkLifecycle;
    AgentLifecycleManager agentLifecycle;
    BackPopulater backPopulator;
    RestartLifecycleManager restartLifecycle;
    SecretsLifecycleManager secretsLifecycle;
    AllocationLifecycleManager allocationLifecycle;
    ServiceLifecycleManager serviceLifecycle;

    public InstanceLifecycleManagerImpl(K8sLifecycleManager k8sLifecycle, VirtualMachineLifecycleManager vmLifecycle, VolumeLifecycleManager volumeLifecycle,
            ObjectManager objectManager, ImageCredentialLookup credLookup, ServiceDao svcDao, TransactionDelegate transaction,
            NetworkLifecycleManager networkLifecycle, AgentLifecycleManager agentLifecycle, BackPopulater backPopulator,
            RestartLifecycleManager restartLifecycle, SecretsLifecycleManager secretsLifecycle, AllocationLifecycleManager allocationLifecycle,
            ServiceLifecycleManager serviceLifecycle) {
        super();
        this.k8sLifecycle = k8sLifecycle;
        this.vmLifecycle = vmLifecycle;
        this.volumeLifecycle = volumeLifecycle;
        this.objectManager = objectManager;
        this.credLookup = credLookup;
        this.svcDao = svcDao;
        this.transaction = transaction;
        this.networkLifecycle = networkLifecycle;
        this.agentLifecycle = agentLifecycle;
        this.backPopulator = backPopulator;
        this.restartLifecycle = restartLifecycle;
        this.secretsLifecycle = secretsLifecycle;
        this.allocationLifecycle = allocationLifecycle;
        this.serviceLifecycle = serviceLifecycle;
    }

    @Override
    public ListenableFuture<?> preCreate(Instance instance) {
        return this.agentLifecycle.create(instance);
    }

    @Override
    public void create(Instance instance) throws LifecycleException {
        k8sLifecycle.instanceCreate(instance);

        Stack stack = setStack(instance);

        networkLifecycle.create(instance, stack);

        lookupRegistryCredential(instance);

        setName(instance);

        setLogConfig(instance);

        Object secretsOpaque = secretsLifecycle.create(instance);

        vmLifecycle.instanceCreate(instance);

        volumeLifecycle.create(instance);

        networkLifecycle.assignNetworkResources(instance);

        saveCreate(instance, secretsOpaque);
    }

    @Override
    public void preStart(Instance instance) throws LifecycleException {
        volumeLifecycle.preStart(instance);

        serviceLifecycle.preStart(instance);

        clearStartSource(instance);

        allocationLifecycle.preStart(instance);

        networkLifecycle.assignNetworkResources(instance);

        objectManager.persist(instance);
    }

    @Override
    public void postStart(Instance instance) {
        backPopulator.update(instance);

        restartLifecycle.postStart(instance);

        objectManager.persist(instance);
    }

    @Override
    public void postStop(Instance instance, boolean stopOnly) {
        allocationLifecycle.postStop(instance);

        restartLifecycle.postStop(instance, stopOnly);

        objectManager.persist(instance);
    }

    @Override
    public void preRemove(Instance instance)  {
        agentLifecycle.preRemove(instance);

        volumeLifecycle.preRemove(instance);

        networkLifecycle.preRemove(instance);

        objectManager.persist(instance);
    }

    @Override
    public void postRemove(Instance instance)  {
        allocationLifecycle.postRemove(instance);

        serviceLifecycle.postRemove(instance);

        objectManager.persist(instance);
    }

    protected void saveCreate(Instance instance, Object secretsOpaque) {
        if (secretsOpaque == null) {
            objectManager.persist(instance);
        } else {
            transaction.doInTransaction(() -> {
                secretsLifecycle.persistCreate(instance, secretsOpaque);
                objectManager.persist(instance);
            });
        }
    }

    private void clearStartSource(Instance instance) {
        setField(instance, InstanceConstants.FIELD_STOP_SOURCE, null);
    }

    private void lookupRegistryCredential(Instance instance) {
        if (instance.getRegistryCredentialId() == null) {
            String uuid = fieldString(instance, InstanceConstants.FIELD_IMAGE);
            Credential cred = credLookup.getDefaultCredential(uuid, instance.getAccountId());
            if (cred != null && cred.getId() != null){
                instance.setRegistryCredentialId(cred.getId());
            }
        }
    }

    protected Stack setStack(Instance instance) {
        Long stackId = instance.getStackId();
        if (stackId != null) {
            return objectManager.loadResource(Stack.class, stackId);
        }
        Stack stack = svcDao.getOrCreateDefaultStack(instance.getAccountId());
        instance.setStackId(stack.getId());
        return stack;
    }


    protected void setName(Instance instance) {
        String name = getLabel(instance, SystemLabels.LABEL_DISPLAY_NAME);
        if (StringUtils.isBlank(name)) {
            name = instance.getName();
        }

        if (StringUtils.isBlank(name)) {
            name = "r-" + instance.getUuid().substring(0, 8);
        }

        instance.setName(name.replaceFirst("/", ""));
    }


    protected void setLogConfig(Instance instance) {
        LogConfig logConfig = DataAccessor.field(instance, InstanceConstants.FIELD_LOG_CONFIG, LogConfig.class);
        if (logConfig != null && !StringUtils.isEmpty(logConfig.getDriver()) && logConfig.getConfig() == null) {
            logConfig.setConfig(new HashMap<>());
            DataAccessor.setField(instance, InstanceConstants.FIELD_LOG_CONFIG, logConfig);
        }
    }

}
