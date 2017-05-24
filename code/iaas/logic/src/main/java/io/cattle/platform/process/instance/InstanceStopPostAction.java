package io.cattle.platform.process.instance;

import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.constants.DockerInstanceConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.transform.DockerTransformer;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.util.DateUtils;

import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class InstanceStopPostAction extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    private static Logger log = LoggerFactory.getLogger(InstanceStopPostAction.class);

    private static long RUNNING_TIME = 10000L;

    @Inject
    DockerTransformer dockerTransformer;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.stop" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();

        String chainProcess = null;
        boolean start = DataAccessor.fromMap(state.getData()).withScope(InstanceProcessOptions.class).withKey(InstanceProcessOptions.START).withDefault(false)
                .as(Boolean.class);

        if (start) {
            chainProcess = InstanceConstants.PROCESS_START;
        } else if (Boolean.TRUE.equals(state.getData().get(InstanceConstants.REMOVE_OPTION)) ||
                InstanceConstants.ON_STOP_REMOVE.equals(instance.getInstanceTriggeredStop())) {
            chainProcess = objectProcessManager.getStandardProcessName(StandardProcess.REMOVE, instance);
        }

        int restartCount = getRestartCount(state, instance);
        boolean shouldRestart = StringUtils.isBlank(chainProcess) &&
                !isUserStopped(instance) &&
                shouldRestart(instance, restartCount);

        state.getData().put("shouldRestartCount", restartCount);

        if (shouldRestart) {
            restartCount++;
        }

        return new HandlerResult(InstanceConstants.FIELD_SHOULD_RESTART, shouldRestart,
                InstanceConstants.FIELD_START_RETRY_COUNT, restartCount)
                .withChainProcessName(chainProcess);
    }

    protected boolean shouldRestart(Instance instance, int restartCount) {
        RestartPolicy rp = DataAccessor.field(instance, DockerInstanceConstants.FIELD_RESTART_POLICY, jsonMapper, RestartPolicy.class);
        if (isStartOnce(instance)) {
            rp = new RestartPolicy();
            rp.setName(RestartPolicy.RESTART_ON_FAILURE);
        }

        if (rp == null) {
            return instance.getServiceId() != null;
        }

        int exitCode = dockerTransformer.getExitCode(instance);
        return checkRestartPolicy(rp, exitCode, restartCount);
    }

    protected int getRestartCount(ProcessState state, Instance instance) {
        Object obj = state.getData().get("shouldRestartCount");
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }

        Integer count = DataAccessor.fieldInteger(instance, InstanceConstants.FIELD_START_RETRY_COUNT);
        if (count == null) {
            count = 0;
        }
        Date started = getDate(instance, DockerInstanceConstants.FIELD_DOCKER_INSPECT, "State", "StartedAt");
        Date finished = getDate(instance, DockerInstanceConstants.FIELD_DOCKER_INSPECT, "State", "FinishedAt");

        if (started == null || finished == null) {
            return count;
        }

        if (started.equals(finished) || finished.before(started)) {
            return count;
        }

        long runningTime = finished.getTime() - started.getTime();
        if (runningTime > RUNNING_TIME) {
            count = 0;
        }

        return count;
    }

    protected Date getDate(Instance instance, String... keys) {
        Object obj = CollectionUtils.getNestedValue(instance.getData(), keys);
        if (obj instanceof String) {
            try {
                return DateUtils.parse((String) obj);
            } catch (DateTimeParseException e) {
                log.error("Failed to parse date [{}]", obj, e);
            }
        }
        return null;
    }

    protected boolean checkRestartPolicy(RestartPolicy rp, int exitCode, int restartCount) {
        if (rp.isNever()) {
            return false;
        } else if (rp.isOnFailure()) {
            if (exitCode == 0) {
                return false;
            }
            if (rp.getMaximumRetryCount() <= 0) {
                return true;
            }
            return restartCount < rp.getMaximumRetryCount();
        } else if (rp.isAlways()) {
            return true;
        }

        // Shouldn't get here, unless we add a new policy name
        return false;
    }

    protected boolean isUserStopped(Instance instance) {
        String source = DataAccessor.fieldString(instance, InstanceConstants.FIELD_STOP_SOURCE);
        return InstanceConstants.ACTION_SOURCE_USER.equals(source);
    }

    protected boolean isStartOnce(Instance instance) {
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        if (labels.get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE) != null) {
            return Boolean.valueOf(((String) labels
                    .get(SystemLabels.LABEL_SERVICE_CONTAINER_START_ONCE)));
        }
        return false;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
