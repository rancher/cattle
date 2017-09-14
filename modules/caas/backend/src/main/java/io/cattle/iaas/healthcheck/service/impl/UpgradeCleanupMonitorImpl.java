package io.cattle.iaas.healthcheck.service.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.task.Task;

import java.util.Date;
import java.util.List;

import org.jooq.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class UpgradeCleanupMonitorImpl extends AbstractJooqDao implements Task {
    private static final Logger log = LoggerFactory.getLogger(UpgradeCleanupMonitorImpl.class);
    private static final DynamicLongProperty REMOVE_DELAY = ArchaiusUtil.getLong("upgrade.cleanup.after");

    ObjectProcessManager objectProcessManager;
    JsonMapper jsonMapper;

    public UpgradeCleanupMonitorImpl(Configuration configuration, ObjectProcessManager objectProcessManager, JsonMapper jsonMapper) {
        super(configuration);
        this.objectProcessManager = objectProcessManager;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void run() {
        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.STATE.ne(CommonStatesConstants.REMOVING))
                    .and(INSTANCE.DESIRED.isFalse())
                    .and(INSTANCE.UPGRADE_TIME.lt(new Date(System.currentTimeMillis() - REMOVE_DELAY.get() * 1000)))
                .fetchInto(InstanceRecord.class);

        for (Instance instance : instances) {
            try {
                objectProcessManager.stopThenRemove(instance, null);
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
