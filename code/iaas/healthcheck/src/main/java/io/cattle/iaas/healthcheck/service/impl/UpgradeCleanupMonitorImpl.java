package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.task.Task;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.jooq.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class UpgradeCleanupMonitorImpl extends AbstractJooqDao implements Task {
    private static final Logger log = LoggerFactory.getLogger(UpgradeCleanupMonitorImpl.class);
    private static final DynamicLongProperty REMOVE_DELAY = ArchaiusUtil.getLong("upgrade.cleanup.after");

    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public void run() {
        Condition removeCondition = SERVICE_EXPOSE_MAP.UPGRADE_TIME.lt(new Date(System.currentTimeMillis()
                - REMOVE_DELAY.get() * 1000));
        List<? extends Instance> instances =
                create()
                        .select(INSTANCE.fields())
                        .from(INSTANCE)
                        .join(SERVICE_EXPOSE_MAP)
                        .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                        .where(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.REMOVING))
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true))
                        .and(removeCondition)
                        .fetchInto(InstanceRecord.class);
        for (Instance instance : instances) {
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
                log.info("Scheduled remove for upgraded instance id [{}]", instance.getId());
            } catch (ProcessInstanceException e) {
                // don't error out so we have a chance to schedule remove for the rest of the instances
                log.info("Failed to schedule remove for upgraded instance id [{}]", instance.getId(), e);
            }
        }
    }


    @Override
    public String getName() {
        return "upgrade.cleanup";
    }
}
