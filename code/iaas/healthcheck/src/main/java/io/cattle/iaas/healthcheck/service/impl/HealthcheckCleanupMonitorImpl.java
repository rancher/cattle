package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.HEALTHCHECK_INSTANCE;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import io.cattle.platform.core.addon.InstanceHealthCheck;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.task.Task;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthcheckCleanupMonitorImpl extends AbstractJooqDao implements Task {
    private static final Logger log = LoggerFactory.getLogger(HealthcheckCleanupMonitorImpl.class);

    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public void run() {
        List<? extends Instance> instances = 
                create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HEALTHCHECK_INSTANCE)
                        .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.notIn(CommonStatesConstants.REMOVING))
                        .and(INSTANCE.HEALTH_STATE.in(HealthcheckConstants.HEALTH_STATE_INITIALIZING,
                                HealthcheckConstants.HEALTH_STATE_REINITIALIZING))
                .fetchInto(InstanceRecord.class);
        for (Instance instance : instances) {
            boolean remove = needToRemove(instance);
            if (!remove) {
                continue;
            }
            try {
                try {
                    objectProcessManager.scheduleStandardProcessAsync(StandardProcess.REMOVE, instance,
                            null);
                } catch (ProcessCancelException e) {
                    if (instance.getState().equalsIgnoreCase(InstanceConstants.STATE_STOPPING)) {
                        continue;
                    }
                    objectProcessManager.scheduleProcessInstanceAsync(InstanceConstants.PROCESS_STOP,
                            instance, ProcessUtils.chainInData(new HashMap<String, Object>(),
                                    InstanceConstants.PROCESS_STOP, InstanceConstants.PROCESS_REMOVE));
                }
                log.info("Scheduled remove for instance id [{}]", instance.getId());
            } catch (ProcessInstanceException e) {
                // don't error out so we have a chance to schedule remove for the rest of the instances
                log.info("Failed to schedule remove for instance id [{}]", instance.getId(), e);
            }
        }
    }

    protected boolean needToRemove(Instance instance) {
        boolean remove = false;
        InstanceHealthCheck healthCheck = DataAccessor.field(instance,
                InstanceConstants.FIELD_HEALTH_CHECK, jsonMapper, InstanceHealthCheck.class);
        Integer timeout;
        if (instance.getHealthState().equalsIgnoreCase(HealthcheckConstants.HEALTH_STATE_INITIALIZING)) {
            timeout = healthCheck.getInitializingTimeout();
        } else {
            timeout = healthCheck.getReinitializingTimeout();
        }
        
        if (timeout != null && instance.getHealthUpdated() != null) {
            long createdTimeAgo = System.currentTimeMillis() - instance.getHealthUpdated().getTime();
            if (createdTimeAgo >= timeout) {
                remove = true;
            }
        }
        return remove;
    }

    @Override
    public String getName() {
        return "healthcheck.cleanup";
    }
}
