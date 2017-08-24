package io.cattle.platform.lifecycle.impl;

import io.cattle.platform.backpopulate.BackPopulater;
import io.cattle.platform.core.addon.RestartPolicy;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.lifecycle.RestartLifecycleManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

import static io.cattle.platform.object.util.DataAccessor.*;

public class RestartLifecycleManagerImpl implements RestartLifecycleManager {

    private static final Logger log = LoggerFactory.getLogger(RestartLifecycleManager.class);

    private static long RUNNING_TIME = 10000L;

    BackPopulater backPopulator;

    public RestartLifecycleManagerImpl(BackPopulater backPopulator) {
        super();
        this.backPopulator = backPopulator;
    }

    @Override
    public void postStop(Instance instance, boolean stopOnly) {
        int exitCode = updateExitCode(instance);
        updateStopTime(instance);
        updateStartRetryCount(instance, stopOnly, exitCode);
    }

    private void updateStopTime(Instance instance) {
        setField(instance, InstanceConstants.FIELD_STOPPED, new Date());
    }

    @Override
    public void postStart(Instance instance) {
        updateStartCount(instance);
        updateFirstRunning(instance);
    }

    private int updateExitCode(Instance instance) {
        int exitCode = backPopulator.getExitCode(instance);
        DataAccessor.setField(instance, InstanceConstants.FIELD_EXIT_CODE, exitCode);
        return exitCode;
    }

    private void updateFirstRunning(Instance instance) {
        if (instance.getFirstRunning() == null) {
            instance.setFirstRunning(new Date());
        }
    }

    private void updateStartCount(Instance instance) {
        Long startCount = instance.getStartCount() == null ? 1 : instance.getStartCount() + 1;
        instance.setStartCount(startCount);
        setField(instance, InstanceConstants.FIELD_LAST_START, new Date());
    }

    private void updateStartRetryCount(Instance instance, boolean stopOnly, int exitCode) {
        int restartCount = getRestartCount(instance);
        boolean shouldRestart = stopOnly &&
                !isUserStopped(instance) &&
                shouldRestart(instance, restartCount, exitCode);

        if (shouldRestart) {
            restartCount++;
        }

        setField(instance, InstanceConstants.FIELD_SHOULD_RESTART, shouldRestart);
        setField(instance, InstanceConstants.FIELD_START_RETRY_COUNT, restartCount);
    }

    protected int getRestartCount(Instance instance) {
        Integer count = fieldInteger(instance, InstanceConstants.FIELD_START_RETRY_COUNT);
        if (count == null) {
            count = 0;
        }
        Date started = getDate(instance, DataAccessor.FIELDS, InstanceConstants.FIELD_DOCKER_INSPECT, "State", "StartedAt");
        Date finished = getDate(instance, DataAccessor.FIELDS, InstanceConstants.FIELD_DOCKER_INSPECT, "State", "FinishedAt");

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

    protected boolean shouldRestart(Instance instance, int restartCount, int exitCode) {
        RestartPolicy rp = DataAccessor.field(instance, InstanceConstants.FIELD_RESTART_POLICY, RestartPolicy.class);
        if (isStartOnce(instance)) {
            rp = new RestartPolicy();
            rp.setName(RestartPolicy.RESTART_NEVER);
        }

        if (rp == null) {
            return instance.getServiceId() != null;
        }

        return checkRestartPolicy(rp, exitCode, restartCount);
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

}
