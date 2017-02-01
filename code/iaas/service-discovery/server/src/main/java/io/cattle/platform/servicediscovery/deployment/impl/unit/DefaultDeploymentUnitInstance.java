package io.cattle.platform.servicediscovery.deployment.impl.unit;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.iaas.api.auditing.AuditEventType;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.servicediscovery.api.resource.ServiceDiscoveryConfigItem;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryDnsUtil;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.InstanceUnit;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.util.exception.InstanceException;
import io.cattle.platform.util.exception.ServiceInstanceAllocateException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class DefaultDeploymentUnitInstance extends DeploymentUnitInstance implements InstanceUnit {
    private static final Set<String> ERROR_STATES = new HashSet<String>(Arrays.asList(
            InstanceConstants.STATE_ERRORING,
            InstanceConstants.STATE_ERROR));
    private static final Set<String> BAD_ALLOCATING_STATES = new HashSet<String>(Arrays.asList(
            InstanceConstants.STATE_ERRORING,
            InstanceConstants.STATE_ERROR,
            InstanceConstants.STATE_STOPPING,
            InstanceConstants.STATE_STOPPED));

    protected String instanceName;
    protected boolean startOnce;
    protected Instance instance;
    protected ServiceIndex serviceIndex;

    public DefaultDeploymentUnitInstance(DeploymentServiceContext context, String uuid,
            Service service, String instanceName, Instance instance, String launchConfigName) {
        super(context, uuid, service, launchConfigName);
        this.instanceName = instanceName;
        this.instance = instance;
        if (this.instance != null) {
            exposeMap = context.exposeMapDao.findInstanceExposeMap(this.instance);
            Long svcIndexId = instance.getServiceIndexId();
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
                    .get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE);
            if (StringUtils.equalsIgnoreCase(startOnceLabel, "true")) {
                startOnce = true;
            }
        }
    }

    @Override
    public boolean isError() {
        return this.instance != null && (ERROR_STATES.contains(this.instance.getState()) || this.instance.getRemoved() != null);
    }

    @Override
    protected void removeUnitInstance() {
        removeInstance(instance, context.objectProcessManager);
    }

    public static void removeInstance(Instance instance, ObjectProcessManager objectProcessManager) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(ServiceConstants.PROCESS_DATA_SERVICE_RECONCILE, true);
        if (!(instance.getState().equals(CommonStatesConstants.REMOVED) || instance.getState().equals(
                CommonStatesConstants.REMOVING))) {
            try {
                objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                        data);
            } catch (ProcessCancelException e) {
                objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                        instance, ProcessUtils.chainInData(data,
                                InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DeploymentUnitInstance create(Map<String, Object> deployParams) {
        if (createNew()) {
            Map<String, Object> launchConfigData = populateLaunchConfigData(deployParams);
            // remove named volumes from the list
            if (launchConfigData.get(InstanceConstants.FIELD_DATA_VOLUMES) != null) {
                List<String> dataVolumes = new ArrayList<>();
                dataVolumes.addAll((List<String>) launchConfigData.get(InstanceConstants.FIELD_DATA_VOLUMES));
                if (deployParams.get(ServiceConstants.FIELD_INTERNAL_VOLUMES) != null) {
                    for (String namedVolume : (List<String>) deployParams
                            .get(ServiceConstants.FIELD_INTERNAL_VOLUMES)) {
                        dataVolumes.remove(namedVolume);
                    }
                    launchConfigData.put(InstanceConstants.FIELD_DATA_VOLUMES, dataVolumes);
                }
                launchConfigData.remove(ServiceConstants.FIELD_INTERNAL_VOLUMES);
            }

            Pair<Instance, ServiceExposeMap> instanceMapPair = context.exposeMapDao.createServiceInstance(launchConfigData,
                    service);
            this.instance = instanceMapPair.getLeft();
            this.exposeMap = instanceMapPair.getRight();
            this.generateAuditLog(AuditEventType.create,
                    ServiceConstants.AUDIT_LOG_CREATE_EXTRA, ActivityLog.INFO);
        }

        this.instance = context.objectManager.reload(this.instance);
        return this;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> populateLaunchConfigData(Map<String, Object> deployParams) {
        Map<String, Object> launchConfigData = ServiceDiscoveryUtil.buildServiceInstanceLaunchData(service,
                deployParams, launchConfigName, context.allocationHelper);
        launchConfigData.put("name", this.instanceName);
        launchConfigData.remove(ServiceDiscoveryConfigItem.RESTART.getCattleName());
        Object labels = launchConfigData.get(InstanceConstants.FIELD_LABELS);
        if (labels != null) {
            String overrideHostName = ((Map<String, String>) labels)
                    .get(ServiceConstants.LABEL_OVERRIDE_HOSTNAME);
            if (StringUtils.equalsIgnoreCase(overrideHostName, "container_name")) {
                String domainName = (String) launchConfigData.get(DockerInstanceConstants.FIELD_DOMAIN_NAME);
                String overrideName = getOverrideHostName(domainName, this.instanceName);
                launchConfigData.put(InstanceConstants.FIELD_HOSTNAME, overrideName);
            }
        }

        launchConfigData.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX_ID,
                this.serviceIndex.getId());
        launchConfigData.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                this.serviceIndex.getServiceIndex());
        launchConfigData.put(InstanceConstants.FIELD_ALLOCATED_IP_ADDRESS, serviceIndex.getAddress());
        return launchConfigData;
    }

    private String getOverrideHostName(String domainName, String instanceName) {
        String overrideName = instanceName;
        if (instanceName != null && instanceName.length() > 64) {
            // legacy code - to support old data where service suffix object wasn't created
            String serviceSuffix = ServiceDiscoveryUtil.getServiceSuffixFromInstanceName(instanceName);
            String serviceSuffixWDivider = instanceName.substring(instanceName.lastIndexOf(serviceSuffix) - 1);
            int truncateIndex = 64 - serviceSuffixWDivider.length();
            if (domainName != null) {
                truncateIndex = truncateIndex - domainName.length() - 1;
            }
            overrideName = instanceName.substring(0, truncateIndex) + serviceSuffixWDivider;
        }
        return overrideName;
    }

    @Override
    public boolean createNew() {
        return this.instance == null;
    }

    @Override
    public DeploymentUnitInstance waitForStartImpl() {
        this.waitForAllocate();

        instance = context.resourceMonitor.waitForNotTransitioning(instance);
        if (!((startOnce && isStartedOnce()) || (InstanceConstants.STATE_RUNNING.equals(instance.getState())))) {
            String error = TransitioningUtils.getTransitioningError(instance);
            String message = String.format("Expected state running but got %s", instance.getState());
            if (org.apache.commons.lang3.StringUtils.isNotBlank(error)) {
                message = message + ": " + error;
            }
            throw new InstanceException(message, instance);
        }

        return this;
    }

    @Override
    protected boolean isStartedImpl() {
        if (startOnce) {
            return isStartedOnce();
        }
        return context.objectManager.reload(this.instance).getState().equalsIgnoreCase(InstanceConstants.STATE_RUNNING);
    }

    protected boolean isStartedOnce() {
        List<String> validStates = Arrays.asList(InstanceConstants.STATE_STOPPED, InstanceConstants.STATE_STOPPING,
                InstanceConstants.STATE_RUNNING);
        return validStates.contains(context.objectManager.reload(this.instance).getState())
                && context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                        instance.getId()).size() > 0;
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
        try {
            if (this.instance != null) {
                if (context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                        instance.getId()).size() > 0) {
                    return;
                }
                instance = context.resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                    @Override
                    public boolean evaluate(Instance obj) {
                        if ((startOnce && ERROR_STATES.contains(obj.getState()))
                                || obj.getRemoved() != null
                                || (!startOnce && BAD_ALLOCATING_STATES.contains(obj.getState()))) {
                            String error = TransitioningUtils.getTransitioningError(obj);
                            String message = "Bad instance [" + key(instance) + "] in state [" + obj.getState() + "]";
                            if (StringUtils.isNotBlank(error)) {
                                message = message + ": " + error;
                            }
                            throw new RuntimeException(message);
                        }
                        return context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                                instance.getId()).size() > 0;
                    }

                    @Override
                    public String getMessage() {
                        return "allocated";
                    }
                });
            }
        } catch (TimeoutException e) {
            throw e;
        } catch (Exception ex) {
            throw new ServiceInstanceAllocateException("Failed to allocate instance [" + key(instance) + "]", ex, this.instance);
        }

    }

    protected String key(Instance instance) {
        Object resourceId = context.idFormatter.formatId(instance.getKind(), instance.getId());
        return String.format("%s:%s", instance.getKind(), resourceId);
    }

    @Override
    public DeploymentUnitInstance startImpl() {
        if (instance != null && InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            context.activityService.instance(instance, "start", "Starting stopped instance", ActivityLog.INFO);
            context.objectProcessManager.scheduleProcessInstanceAsync(
                    InstanceConstants.PROCESS_START, instance, null);
        }
        return this;
    }

    @Override
    public ServiceIndex getServiceIndex() {
        return this.serviceIndex;
    }

    @SuppressWarnings("unchecked")
    protected ServiceIndex createServiceIndex() {
        // create index
        Stack stack = context.objectManager.loadResource(Stack.class, service.getStackId());
        String serviceIndex = ServiceDiscoveryUtil.getGeneratedServiceIndex(stack, service, launchConfigName,
                instanceName);
        ServiceIndex serviceIndexObj = context.serviceDao.createServiceIndex(service, launchConfigName, serviceIndex);

        // allocate ip address if not set
        if (DataAccessor.fieldBool(service, ServiceConstants.FIELD_SERVICE_RETAIN_IP)) {
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
            context.sdService.allocateIpToServiceIndex(service, serviceIndexObj, requestedIp);
        }

        return serviceIndexObj;
    }

    @Override
    public List<String> getSearchDomains() {
        String stackNamespace = ServiceDiscoveryDnsUtil.getStackNamespace(this.stack, this.service);
        String serviceNamespace = ServiceDiscoveryDnsUtil
                .getServiceNamespace(this.stack, this.service);
        return Arrays.asList(stackNamespace, serviceNamespace);
    }

    @Override
    public Long getCreateIndex() {
        Long createIndex = this.instance == null ? null : this.instance.getCreateIndex();
        return createIndex;
    }

    @Override
    public void scheduleCreate() {
        if (instance.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, instance,
                    null);
        }

        if (exposeMap.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            context.objectProcessManager.scheduleStandardProcessAsync(StandardProcess.CREATE, exposeMap,
                    null);
        }
    }

    @Override
    public boolean isTransitioning() {
        if (this.instance == null) {
            return false;
        }
        return context.objectMetaDataManager.isTransitioningState(Instance.class, this.instance.getState());
    }
}

