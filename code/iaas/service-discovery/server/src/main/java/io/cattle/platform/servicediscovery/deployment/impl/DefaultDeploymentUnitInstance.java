package io.cattle.platform.servicediscovery.deployment.impl;

import static io.cattle.platform.core.model.tables.ImageTable.IMAGE;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.resource.ServiceDiscoveryConfigItem;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class DefaultDeploymentUnitInstance extends DeploymentUnitInstance {
    protected String instanceName;
    protected ServiceExposeMap exposeMap;

    public DefaultDeploymentUnitInstance() {
        super(null, null, null);
    }

    public DefaultDeploymentUnitInstance(String uuid, Service service,
            String instanceName,
            Instance instance, DeploymentServiceContext context) {
        super(uuid, service, context);
        this.instanceName = instanceName;
        this.instance = instance;
        if (this.instance != null) {
            exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
        }
    }

    @Override
    public boolean isError() {
        return this.instance.getRemoved() != null;
    }

    @Override
    public void remove() {
        // 1) remove the mapping
        List<? extends ServiceExposeMap> maps = context.objectManager.mappedChildren(
                context.objectManager.loadResource(Instance.class, instance.getId()),
                ServiceExposeMap.class);
        for (ServiceExposeMap map : maps) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, map, null);
        }
        // 2) schedule remove for the instance
        if (!(instance.getState().equals(CommonStatesConstants.REMOVED)
        || instance.getState().equals(CommonStatesConstants.REMOVING))) {
            try {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                        null);
            } catch (ProcessCancelException e) {
                Map<String, Object> data = new HashMap<>();
                data.put(InstanceConstants.REMOVE_OPTION, true);
                context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance,
                        data);
            }
        }
    }

    @Override
    public DeploymentUnitInstance start(List<Integer> volumesFromInstancesIds) {
        if (this.instance == null) {
            Map<String, Object> launchConfigData = context.sdService.buildLaunchData(service, labels, instanceName,
                    volumesFromInstancesIds);
            Object registryCredentialId = launchConfigData.get(ServiceDiscoveryConfigItem.REGISTRYCREDENTIALID
                    .getCattleName());
            Long imageId = getImage(String.valueOf(launchConfigData.get(InstanceConstants.FIELD_IMAGE_UUID)),
                    registryCredentialId != null ? (Integer) registryCredentialId : null);
            List<Long> networkIds = context.sdService.getServiceNetworkIds(service);

            Map<Object, Object> properties = new HashMap<Object, Object>();
            properties.putAll(launchConfigData);
            properties.put(INSTANCE.ACCOUNT_ID, service.getAccountId());
            properties.put(INSTANCE.KIND, InstanceConstants.KIND_CONTAINER);
            properties.put(InstanceConstants.FIELD_NETWORK_IDS, networkIds);
            properties.put(INSTANCE.IMAGE_ID, imageId);
            Pair<Instance, ServiceExposeMap> instanceMapPair = context.exposeMapDao.createServiceInstance(properties,
                    service,
                    instanceName);
            this.instance = instanceMapPair.getLeft();
            this.exposeMap = instanceMapPair.getRight();
        }
        context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, instance,
                null);
        context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                null);

        if (InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            context.objectProcessManager.scheduleProcessInstanceAsync(
                            InstanceConstants.PROCESS_START, instance, null);
        }
        this.instance = context.objectManager.reload(this.instance);
        return this;
    }

    @Override
    public DeploymentUnitInstance waitForStart() {
        this.instance = context.resourceMonitor.waitFor(context.objectManager.reload(this.instance),
                new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return InstanceConstants.STATE_RUNNING.equals(obj.getState());
            }
        });
        return this;
    }

    @Override
    public void stop() {
        context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                null);
    }

    @Override
    public boolean isStarted() {
        return context.objectManager.reload(this.instance).getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING);
    }

    @Override
    public boolean needsCleanup() {
        // TODO implement when healthcheck comes in
        return false;
    }

    protected Long getImage(String imageUuid, Integer registryCredentialId) {
        Image image;
        try {
            image = context.storageService.registerRemoteImage(imageUuid);
            if (image == null) {
                return null;
            }
            if (registryCredentialId != null) {
                context.objectManager.setFields(image, IMAGE.REGISTRY_CREDENTIAL_ID, registryCredentialId);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to get image [" + imageUuid + "]");
        }

        return image == null ? null : image.getId();
    }
}

