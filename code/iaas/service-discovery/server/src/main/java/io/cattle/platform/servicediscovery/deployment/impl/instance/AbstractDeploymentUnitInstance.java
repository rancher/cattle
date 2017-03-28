
package io.cattle.platform.servicediscovery.deployment.impl.instance;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
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
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstance;
import io.cattle.platform.servicediscovery.deployment.impl.manager.DeploymentUnitManagerImpl.DeploymentUnitManagerContext;
import io.cattle.platform.util.exception.InstanceException;
import io.cattle.platform.util.exception.ServiceInstanceAllocateException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractDeploymentUnitInstance implements DeploymentUnitInstance {
    private static final Set<String> ERROR_STATES = new HashSet<String>(Arrays.asList(
            InstanceConstants.STATE_ERRORING,
            InstanceConstants.STATE_ERROR));
    private static final Set<String> BAD_ALLOCATING_STATES = new HashSet<String>(Arrays.asList(
            InstanceConstants.STATE_ERRORING,
            InstanceConstants.STATE_ERROR,
            InstanceConstants.STATE_STOPPING,
            InstanceConstants.STATE_STOPPED));
    protected static final List<String> RESTART_ALWAYS_POLICY_NAMES = Arrays.asList("always", "unless-stopped");
    protected static final int UNLIMITED_RETRIES = -1;

    protected DeploymentUnitManagerContext context;

    protected Instance instance;
    protected String instanceName;
    protected boolean startOnFailure;
    protected int maximumRetryCount = UNLIMITED_RETRIES;
    protected String launchConfigName;

    public AbstractDeploymentUnitInstance(DeploymentUnitManagerContext context, String instanceName, Instance instance,
            String launchConfigName) {
        this.context = context;
        this.instanceName = instanceName;
        this.instance = instance;
        this.launchConfigName = launchConfigName;
        setStartOnFailure();
    }

    protected abstract void createImpl(Map<String, Object> deployParams);

    public abstract void scheduleCreate();
    
    protected abstract void removeImpl();

    protected abstract boolean isRestartAlways();

    @Override
    public void remove(String reason, String level) {
        String error = TransitioningUtils.getTransitioningError(instance);
        if (StringUtils.isNotBlank(error)) {
            reason = reason + ": " + error;
        }
        this.generateAuditLog(AuditEventType.delete, reason, level);
        removeInstance(this.instance, this.context.objectProcessManager);
        removeImpl();
    }

    @Override
    public void stop() {
        if (instance != null && instance.getState().equals(InstanceConstants.STATE_RUNNING)) {
            context.objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP, instance,
                    null);
        }
    }

    @Override
    public DeploymentUnitInstance start(boolean isDependee) {
        if (this.isStarted(isDependee)) {
            return this;
        }
        if (instance != null && InstanceConstants.STATE_STOPPED.equals(instance.getState())) {
            context.activityService.instance(instance, "start", "Starting stopped instance", ActivityLog.INFO);
            context.objectProcessManager.scheduleProcessInstanceAsync(
                    InstanceConstants.PROCESS_START, instance, null);
        }
        return this;
    }

    @Override
    public void waitForStart(boolean isDependee) {
        if (this.isStarted(isDependee)) {
            return;
        }
        this.waitForAllocate();
        instance = context.resourceMonitor.waitForNotTransitioning(instance);
        if (!((startOnFailure && !needRestartOnFailure()) || (InstanceConstants.STATE_RUNNING.equals(instance
                .getState())))) {
            String message = getErrorMessage(String.format("Expected state running but got %s",
                    instance.getState()));
            throw new InstanceException(message, instance);
        }
    }

    public String getErrorMessage(String message) {
        String error = TransitioningUtils.getTransitioningError(instance);
        if (org.apache.commons.lang3.StringUtils.isNotBlank(error)) {
            message = message + ": " + error;
        }
        return message;
    }

    @Override
    public void waitForStop() {
        instance = context.resourceMonitor.waitForNotTransitioning(instance);
        if (!InstanceConstants.STATE_STOPPED.equals(instance
                .getState())) {
            String message = getErrorMessage(String.format("Expected state stopped but got %s",
                    instance.getState()));
            throw new InstanceException(message, instance);
        }
    }

    @Override
    public void waitForHealthy() {
        context.resourceMonitor.waitFor(this.instance, new ResourcePredicate<Instance>() {
            @Override
            public boolean evaluate(Instance obj) {
                if (obj.getHealthState() == null) {
                    return true;
                }
                // only evaluate against init state here
                // unhealthy unit determination is done by service reconcile code
                // and cleanup will be driven by cleanup(boolean) flag
                return !HealthcheckConstants.isInit(obj.getHealthState());
            }

            @Override
            public String getMessage() {
                return "healthy";
            }
        });
    }

    @Override
    public boolean isStarted(boolean isDependee) {
        if (startOnFailure) {
            return !needRestartOnFailure();
        }
        boolean isRunning = context.objectManager.reload(this.instance).getState()
                .equalsIgnoreCase(InstanceConstants.STATE_RUNNING);
        boolean stoppedFromApi = InstanceConstants.ACTION_SOURCE_API.equalsIgnoreCase(DataAccessor.fieldString(
                this.instance, InstanceConstants.FIELD_STOP_SOURCE));

        boolean skipStartOnCreate = !DataAccessor.fieldBool(instance, InstanceConstants.FIELD_START_ON_CREATE)
                && instance.getFirstRunning() == null;

        boolean skipRestart = (stoppedFromApi || skipStartOnCreate || !isRestartAlways()) && !isDependee;
        return isRunning || (!isRunning && skipRestart);
    }

    protected boolean needRestartOnFailure() {
        String instanceState = context.objectManager.reload(this.instance).getState();
        if (instanceState.equalsIgnoreCase(InstanceConstants.STATE_RUNNING)) {
            return false;
        }
        if (context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                instance.getId()).size() == 0) {
            return true;
        }
        if (!Arrays.asList(InstanceConstants.STATE_STOPPED, InstanceConstants.STATE_STOPPING).contains(instanceState)) {
            return true;
        }

        // Reading from the field is for solely test purpose
        // as it is problematic to mock dockerInspect field
        Integer exitCode = 0;
        if (DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_EXIT_CODE) != null) {
            exitCode = DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_EXIT_CODE);
        } else {
            exitCode = context.transformer.getExitCode(instance);
        }

        if (exitCode.equals(0)) {
            return false;
        }
        if (maximumRetryCount == UNLIMITED_RETRIES) {
            return true;
        }

        int startRetryCount = 0;
        if (DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_START_RETRY_COUNT) != null) {
            startRetryCount = DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_START_RETRY_COUNT);
        }
        return startRetryCount < maximumRetryCount;
    }

    @Override
    public boolean isUnhealthy() {
        if (instance != null) {
            if (instance.getHealthState() == null) {
                return false;
            }
            return HealthcheckConstants.isUnhealthy(instance.getHealthState());
        }
        return false;
    }


    @Override
    public String getLaunchConfigName() {
        return launchConfigName;
    }

    protected void waitForAllocate() {
        try {
            if (this.instance != null) {
                if (context.objectManager.find(InstanceHostMap.class, INSTANCE_HOST_MAP.INSTANCE_ID,
                        instance.getId()).size() > 0) {
                    return;
                }
                instance = context.resourceMonitor.waitFor(instance, new ResourcePredicate<Instance>() {
                    @Override
                    public boolean evaluate(Instance obj) {
                        if ((startOnFailure && ERROR_STATES.contains(obj.getState()))
                                || obj.getRemoved() != null
                                || (!startOnFailure && BAD_ALLOCATING_STATES.contains(obj.getState()))) {
                            String error = TransitioningUtils.getTransitioningError(obj);
                            String message = "Bad instance [" + key(instance) + "] in state [" + obj.getState() + "]";
                            if (StringUtils.isNotBlank(error)) {
                                message = message + ": " + error;
                            }
                            throw new ServiceInstanceAllocateException("Failed to allocate instance [" + key(instance)
                                    + "]", null,
                                    instance);
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
            throw new ServiceInstanceAllocateException("Failed to allocate instance [" + key(instance) + "]", ex,
                    this.instance);
        }
    }

    protected void generateAuditLog(AuditEventType eventType, String description, String level) {
        context.activityService.instance(instance, eventType.toString(), description, level);
    }

    @Override
    public Instance getInstance() {
        return instance;
    }
    protected String key(Instance instance) {
        Object resourceId = context.idFormatter.formatId(instance.getKind(), instance.getId());
        return String.format("%s:%s", instance.getKind(), resourceId);
    }

    protected void setStartOnFailure() {
        if (instance != null) {
            boolean startOnce = false;
            Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
            if (labels != null) {
                String startOnceLabel = "false";
                if (labels.get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE) != null) {
                    startOnceLabel = labels.get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE).toString();
                }
                if (StringUtils.equalsIgnoreCase(startOnceLabel, "true")) {
                    startOnFailure = true;
                    // -1 = unlimited to support label driven startOnce behavior
                    maximumRetryCount = UNLIMITED_RETRIES;
                    startOnce = true;
                }
            }

            if (!startOnce) {
                // read start on failure
                RestartPolicy rp = DataAccessor.field(instance, DockerInstanceConstants.FIELD_RESTART_POLICY,
                        context.jsonMapper,
                        RestartPolicy.class);
                if (rp == null || !rp.getName().equalsIgnoreCase(RestartPolicy.RESTART_ON_FAILURE)) {
                    return;
                }
                startOnFailure = true;
                maximumRetryCount = rp.getMaximumRetryCount();
            }
        }
    }


    @Override
    public void create(Map<String, Object> deployParams) {
        createImpl(deployParams);
        setStartOnFailure();
    }

    public static void removeInstance(Instance instance, ObjectProcessManager objectProcessManager) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(ServiceConstants.PROCESS_DATA_SERVICE_RECONCILE, true);
        List<String> ignoreStates = Arrays.asList(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING,
                CommonStatesConstants.PURGED, CommonStatesConstants.PURGING);
        if (!ignoreStates.contains(instance.getState())) {
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
    public boolean isHealthy() {
        return HealthcheckConstants.isHealthy(this.instance.getHealthState());
    }
}
