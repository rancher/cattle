package io.cattle.platform.core.cleanup;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.AuditLogTable.*;
import static io.cattle.platform.core.model.tables.ConfigItemStatusTable.*;
import static io.cattle.platform.core.model.tables.ContainerEventTable.*;
import static io.cattle.platform.core.model.tables.ProcessExecutionTable.*;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceEventTable.*;
import static io.cattle.platform.core.model.tables.ServiceLogTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.ResultQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;


public class TableCleanUp extends AbstractJooqDao {

    private static final Logger log = LoggerFactory.getLogger(TableCleanUp.class);
    private static final DynamicLongProperty PROCESS_INSTANCE_TIME = ArchaiusUtil.getLong("process_instance.purge.after.seconds");
    private static final DynamicLongProperty EVENT_TIME = ArchaiusUtil.getLong("events.purge.after.seconds");
    private static final DynamicLongProperty AUDIT_LOG_TIME = ArchaiusUtil.getLong("audit_log.purge.after.seconds");
    private static final DynamicLongProperty SERVICE_LOG_TIME = ArchaiusUtil.getLong("service_log.purge.after.seconds");

    protected Condition expired(Field<Date> field, DynamicLongProperty property) {
        return field.lt(new Date(System.currentTimeMillis() - (property.get() * 1000)));
    }

    protected void processes() {
        int count = create()
                .delete(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.END_TIME.isNotNull()
                        .and(expired(PROCESS_INSTANCE.END_TIME, PROCESS_INSTANCE_TIME)))
                .execute();
        status(PROCESS_INSTANCE.getName(), count);

        count = create()
                .delete(PROCESS_EXECUTION)
                .where(PROCESS_EXECUTION.CREATED.isNull()
                        .or(expired(PROCESS_EXECUTION.CREATED, PROCESS_INSTANCE_TIME)))
                .execute();
        status(PROCESS_EXECUTION.getName(), count);
    }

    protected void status(String table, int count) {
        if (count <= 0) {
            return;
        }

        log.info("Deleted {} records from {}", count, table);
    }

    protected void events() {
        int count = create()
                .delete(SERVICE_EVENT)
                .where(expired(SERVICE_EVENT.CREATED, EVENT_TIME)
                        .and(SERVICE_EVENT.STATE.eq(CommonStatesConstants.CREATED)))
                .execute();
        status(SERVICE_EVENT.getName(), count);

        count = create()
                .delete(CONTAINER_EVENT)
                .where(expired(CONTAINER_EVENT.CREATED, EVENT_TIME)
                        .and(CONTAINER_EVENT.STATE.eq(CommonStatesConstants.CREATED)))
                .execute();
        status(CONTAINER_EVENT.getName(), count);
    }

    protected void audit() {
        int count = create()
                .delete(AUDIT_LOG)
                .where(expired(AUDIT_LOG.CREATED, AUDIT_LOG_TIME))
                .execute();
        status(AUDIT_LOG.getName(), count);
    }

    protected void serviceLog() {
        int count = create()
                .delete(SERVICE_LOG)
                .where(expired(SERVICE_LOG.CREATED, SERVICE_LOG_TIME))
                .execute();
        status(SERVICE_LOG.getName(), count);
    }

    protected void configItems() {
        int count = deleteConfigItemStatus(create()
                .select(CONFIG_ITEM_STATUS.ID)
                .from(CONFIG_ITEM_STATUS)
                .join(AGENT)
                    .on(CONFIG_ITEM_STATUS.AGENT_ID.eq(AGENT.ID))
                .where(AGENT.REMOVED.isNotNull())
                .limit(100));
        status(CONFIG_ITEM_STATUS.getName(), count);

        count = deleteConfigItemStatus(create()
                .select(CONFIG_ITEM_STATUS.ID).from(CONFIG_ITEM_STATUS)
                .join(SERVICE)
                    .on(CONFIG_ITEM_STATUS.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE.REMOVED.isNotNull())
                .limit(100));
        status(CONFIG_ITEM_STATUS.getName(), count);

        count = deleteConfigItemStatus(create()
                .select(CONFIG_ITEM_STATUS.ID)
                .from(CONFIG_ITEM_STATUS)
                .join(ACCOUNT)
                    .on(CONFIG_ITEM_STATUS.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(ACCOUNT.REMOVED.isNotNull())
                .limit(100));
        status(CONFIG_ITEM_STATUS.getName(), count);
    }

    public void cleanUp(){
        processes();
        events();
        configItems();
        serviceLog();
    }

    private int deleteConfigItemStatus(ResultQuery<Record1<Long>> query) {
        Result<Record1<Long>> toDelete;
        int deletedConfigItemStatuses = 0;
        while ((toDelete = query.fetch()).size() > 0) {
            List<Long> idsToDelete = new ArrayList<>();
            for (Record1<Long> record : toDelete) {
                idsToDelete.add(record.value1());
            }
            deletedConfigItemStatuses += create()
                    .delete(CONFIG_ITEM_STATUS)
                    .where(CONFIG_ITEM_STATUS.ID.in(idsToDelete))
                    .execute();
        }
        return deletedConfigItemStatuses;
    }
}
