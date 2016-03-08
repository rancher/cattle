package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class DefaultDeploymentUnitInstance extends DeploymentUnitInstance implements InstanceUnit {
    protected String instanceName;
    protected boolean startOnce;
    protected Instance instance;
    protected ServiceIndex serviceIndex;

    public DefaultDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Instance instance, Map<String, String> labels, String launchConfigName) {
        super(context, uuid, service, launchConfigName);
        this.instanceName = instanceName;
        this.instance = instance;
        if (this.instance != null) {
            exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
            Long svcIndexId = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID);
            if (svcIndexId != null) {
                serviceIndex = context.objectManager
                        .loadResource(ServiceIndex.class, svcIndexId);
            }
        } else {
            this.serviceIndex = createServiceIndex();
        }
        setStartOnce(service, launchConfigName);
    }

    @SuppressWarnings("unchecked")
    public void setStartOnce(Service service, String launghConfig) {
        Object serviceLabels = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                InstanceConstants.FIELD_LABELS);
        if (serviceLabels != null) {
            String startOnceLabel = ((Map<String, String>) serviceLabels)
                    .get(ServiceDiscoveryConstants.LABEL_SERVICE_CONTAINER_START_ONCE);
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
                context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                        null);
            } catch (ProcessCancelException e) {
                context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
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

        launchConfigData.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID,
                this.serviceIndex.getId());
        launchConfigData.put(InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS, serviceIndex.getAddress());
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

    @Override
    public Instance getInstance() {
        return instance;
    }

    @Override
    public boolean isUnhealthy() {
        if (instance != null) {
            if (instance.getHealthState() == null) {
                return false;
            }
            boolean unhealthyState = instance.getHealthState().equalsIgnoreCase(
                    HealthcheckConstants.HEALTH_STATE_UNHEALTHY) || instance.getHealthState().equalsIgnoreCase(
                    HealthcheckConstants.HEALTH_STATE_UPDATING_UNHEALTHY);
            return unhealthyState;
        }
        return false;
    }

    @Override
    public void stop() {
        if (instance != null && instance.getState().equals(InstanceConstants.STATE_RUNNING)) {
            context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                    null);
        }
    }

    @Override
    public boolean isHealthCheckInitializing() {
        return instance != null && instance.getHealthState() != null
                && HealthcheckConstants.isInit(instance.getHealthState());
    }

    @Override
    public void waitForAllocate() {
        if (this.instance != null) {
            instance = context.resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                            instance.getId()).size() > 0;
                }
            });
        }
    }

    @Override
    public DeploymentUnitInstance startImpl() {
        if (instance != null && InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            context.objectProcessManager.scheduleProcessInstanceAsync(
                    InstanceConstants.PROCESS_START, instance, null);
        }
        return this;
    }

    public ServiceIndex getServiceIndex() {
        return this.serviceIndex;
    }

    @SuppressWarnings("unchecked")
    protected ServiceIndex createServiceIndex() {
        // create index
        Environment stack = context.objectManager.loadResource(Environment.class, service.getEnvironmentId());
        String serviceIndex = ServiceDiscoveryUtil.getGeneratedServiceIndex(stack, service, launchConfigName,
                instanceName);
        ServiceIndex serviceIndexObj = context.serviceDao.createServiceIndex(service, launchConfigName, serviceIndex);
        
        // allocate ip address if not set
        if (DataAccessor.fieldBool(service, ServiceDiscoveryConstants.FIELD_SERVICE_RETAIN_IP)) {
            Object requestedIpObj = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                    InstanceConstants.FIELD_REQUESTED_IP_ADDRESS);
            String requestedIp = null;
            if (requestedIpObj != null) {
                requestedIp = requestedIpObj.toString();
            } else {
                // can be passed via labels
                Object labels = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                        InstanceConstants.FIELD_LABELS);
                if (labels != null) {
                    requestedIp = ((Map<String, String>) labels).get(SystemLabels.LABEL_REQUESTED_IP);
                }
            }
            context.sdService.allocateIpToServiceIndex(serviceIndexObj, requestedIp);
        }
        
        return serviceIndexObj;
    }

    @Override
    public void waitForScheduleStop() {
        this.instance = context.resourceMonitor.waitFor(this.instance,
                new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                        return InstanceConstants.STATE_STOPPING.equals(obj.getState())
                                || InstanceConstants.STATE_STOPPED.equals(obj.getState());
            }
        });
    }

    @Override
    public boolean isIgnore() {
        List<String> errorStates = Arrays.asList(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING);
        return this.instance != null && errorStates.contains(this.instance.getState());
    }
}

