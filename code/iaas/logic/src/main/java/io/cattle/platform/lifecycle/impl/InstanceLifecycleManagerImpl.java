package io.cattle.platform.lifecycle.impl;

import static io.cattle.platform.object.util.DataAccessor.*;

import io.cattle.platform.backpopulate.BackPopulater;
import io.cattle.platform.core.addon.LogConfig;
import io.cattle.platform.core.constants.DockerInstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.api.model.DockerBuild;
import io.cattle.platform.json.JsonMapper;
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

import java.util.HashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class InstanceLifecycleManagerImpl implements InstanceLifecycleManager {

    @Inject
    K8sLifecycleManager k8sLifecycle;
    @Inject
    VirtualMachineLifecycleManager vmLifecycle;
    @Inject
    VolumeLifecycleManager volumeLifecycle;
    @Inject
    ObjectManager objectManager;
    @Inject
    ImageCredentialLookup credLookup;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    ServiceDao svcDao;
    @Inject
    TransactionDelegate transaction;
    @Inject
    NetworkLifecycleManager networkLifecycle;
    @Inject
    AgentLifecycleManager agentLifecycle;
    @Inject
    BackPopulater backPopulator;
    @Inject
    RestartLifecycleManager restartLifecycle;
    @Inject
    SecretsLifecycleManager secretsLifecycle;
    @Inject
    AllocationLifecycleManager allocationLifecycle;
    @Inject
    ServiceLifecycleManager serviceLifecycle;
    @Inject
    InstanceDao instanceDao;

    @Override
    public void create(Instance instance) throws LifecycleException {
        k8sLifecycle.instanceCreate(instance);

        Stack stack = setStack(instance);

        networkLifecycle.create(instance, stack);

        lookupRegistryCredential(instance);

        setName(instance);

        setLogConfig(instance);

        setSystemLabel(instance);

        setupDockerBuild(instance);

        Object secretsOpaque = secretsLifecycle.create(instance);

        vmLifecycle.instanceCreate(instance);

        agentLifecycle.create(instance);

        volumeLifecycle.create(instance);

        networkLifecycle.assignNetworkResources(instance);

        saveCreate(instance, secretsOpaque);
    }

    @Override
    public void preStart(Instance instance) throws LifecycleException {
        volumeLifecycle.preStart(instance);

        clearStartSource(instance);

        networkLifecycle.preStart(instance);

        allocationLifecycle.preStart(instance);

        networkLifecycle.assignNetworkResources(instance);

        objectManager.persist(instance);

        instanceDao.clearCacheInstanceData(instance.getId());
    }

    @Override
    public void postStart(Instance instance) throws LifecycleException {
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

        serviceLifecycle.preRemove(instance);

        volumeLifecycle.preRemove(instance);

        allocationLifecycle.preRemove(instance);

        objectManager.persist(instance);
    }

    @Override
    public void postRemove(Instance instance)  {
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
            String uuid = fieldString(instance, InstanceConstants.FIELD_IMAGE_UUID);
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
        LogConfig logConfig = DataAccessor.field(instance,
                InstanceConstants.FIELD_LOG_CONFIG, jsonMapper, LogConfig.class);
        if (logConfig != null && !StringUtils.isEmpty(logConfig.getDriver()) && logConfig.getConfig() == null) {
            logConfig.setConfig(new HashMap<String, String>());
            DataAccessor.setField(instance, InstanceConstants.FIELD_LOG_CONFIG, logConfig);
        }
    }

    protected void setSystemLabel(Instance instance) {
        if(Boolean.TRUE.equals(instance.getSystem()) &&
                StringUtils.isBlank(getLabel(instance, SystemLabels.LABEL_CONTAINER_SYSTEM))) {
            setLabel(instance, SystemLabels.LABEL_CONTAINER_SYSTEM, "true");
        }
    }

    private void setupDockerBuild(Instance instance) {
        DockerBuild build = field(instance, DockerInstanceConstants.FIELD_BUILD,
                jsonMapper, DockerBuild.class);

        if (build == null) {
            return;
        }

        String imageUuid = fieldString(instance, InstanceConstants.FIELD_IMAGE_UUID);
        if (imageUuid != null) {
            String tag = StringUtils.removeStart(imageUuid, "docker:");
            build.setTag(tag);
        }

        setField(instance, DockerInstanceConstants.FIELD_BUILD, build);
    }

}
