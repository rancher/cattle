package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.AbstractInstanceUnit;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class DefaultDeploymentUnitInstance extends AbstractInstanceUnit {
    protected String instanceName;
    protected boolean startOnce;

    public DefaultDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Instance instance, Map<String, String> labels, String launchConfigName) {
        super(context, uuid, service, launchConfigName);
        this.instanceName = instanceName;
        this.instance = instance;
        if (this.instance != null) {
            exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
        }
        setStartOnce(service, launchConfigName);
    }

    @SuppressWarnings("unchecked")
    public void setStartOnce(Service service, String launghConfig) {
        Object serviceLabels = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                InstanceConstants.FIELD_LABELS);
        if (serviceLabels != null) {
            String startOnceLabel = ((Map<String, String>) serviceLabels)
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_CONTAINER_CREATE_ONLY);
            if (StringUtils.equalsIgnoreCase(startOnceLabel, "true")) {
                startOnce = true;
            }
        }
    }

    @Override
    public boolean isError() {
        return this.instance != null && this.instance.getRemoved() != null;
    }

    @Override
    protected void removeUnitInstance() {
        if (!(instance.getState().equals(CommonStatesConstants.REMOVED) || instance.getState().equals(
                CommonStatesConstants.REMOVING))) {
            try {
                context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            } catch (ProcessCancelException e) {
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                        null);
            }
        }
    }

    @Override
    public DeploymentUnitInstance create(Map<String, Object> deployParams) {
        if (createNew()) {
            Map<String, Object> launchConfigData = populateLaunchConfigData(deployParams);
            Pair<Instance, ServiceExposeMap> instanceMapPair = context.exposeMapDao.createServiceInstance(launchConfigData,
                    service);
            this.instance = instanceMapPair.getLeft();
            this.exposeMap = instanceMapPair.getRight();
        }

        if (instance.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, instance,
                    null);
        }

        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                    null);
        }

        this.instance = context.objectManager.reload(this.instance);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> populateLaunchConfigData(Map<String, Object> deployParams) {
        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.buildServiceInstanceLaunchData(service,
                deployParams, launchConfigName, context.allocatorService);
        launchConfigData.put("name", this.instanceName);
        launchConfigData.remove(ServiceDiscoveryConfigItem.RESTART.getCattleName());
        Object labels = launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            String overrideHostName = ((Map<String, String>) labels)
                    .get(ServiceDiscoveryConstants.LABEL_OVERRIDE_HOSTNAME);
            if (StringUtils.equalsIgnoreCase(overrideHostName, "container_name")) {
                String domainName = (String) launchConfigData.get(DockerInstanceConstants.FIELD_DOMAIN_NAME);
                String overrideName = getOverrideHostName(domainName, this.instanceName);
                launchConfigData.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
            }
        }
        return launchConfigData;
    }

    private String getOverrideHostName(String domainName, String instanceName) {
        String overrideName = instanceName;
        if (instanceName != null && instanceName.length() > 64) {
            String serviceNumber = instanceName.substring(instanceName.lastIndexOf("_"));
            int truncateIndex = 64 - serviceNumber.length();
            if (domainName != null) {
                truncateIndex = truncateIndex - domainName.length() - 1;
            }
            overrideName = instanceName.substring(0, truncateIndex) + serviceNumber;
        }
        return overrideName;
    }

    @Override
    public boolean createNew() {
        return this.instance == null;
    }

    @Override
    public DeploymentUnitInstance waitForStartImpl() {
        this.instance = context.resourceMonitor.waitFor(this.instance,
                new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                return InstanceConstants.STATE_RUNNING.equals(obj.getState());
            }
        });
        return this;
    }

    @Override
    protected boolean isStartedImpl() {
        if (startOnce) {
            List<String> validStates = Arrays.asList(InstanceConstants.STATE_STOPPED, InstanceConstants.STATE_STOPPING,
                    InstanceConstants.STATE_RUNNING);
            return validStates.contains(context.objectManager.reload(this.instance).getState());
        }
        return context.objectManager.reload(this.instance).getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING);
    }

    @Override
    public void waitForNotTransitioning() {
        if (this.instance != null) {
            this.instance = context.resourceMonitor.waitForNotTransitioning(this.instance);
        }
    }
}

