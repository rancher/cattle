package io.cattle.platform.core.cleanup;

import static io.cattle.platform.core.model.tables.AgentTable.AGENT;
import static io.cattle.platform.core.model.tables.ContainerEventTable.CONTAINER_EVENT;
import static io.cattle.platform.core.model.tables.ConfigItemStatusTable.CONFIG_ITEM_STATUS;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.PROCESS_INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceEventTable.SERVICE_EVENT;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import static io.cattle.platform.core.model.tables.AuditLogTable.AUDIT_LOG;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.ResultQuery;

import com.netflix.config.DynamicLongProperty;


public class TableCleanUp extends AbstractJooqDao {

    private static final Log logger = LogFactory.getLog(TableCleanUp.class);
    private static final DynamicLongProperty PROCESS_INSTANCE_TIME = ArchaiusUtil.getLong("process_instance.purge.after.seconds");
    private static final DynamicLongProperty EVENT_TIME = ArchaiusUtil.getLong("events.purge.after.seconds");
    private static final DynamicLongProperty AUDIT_LOG_TIME = ArchaiusUtil.getLong("audit_log.purge.after.seconds");


    public void cleanUp(){
        logger.debug("Starting clean up for PROCESS_INSTANCE table, SERVICE_EVENT table, and CONTAINER_EVENT table.");
        long startProcessInstance = System.currentTimeMillis();

        int deletedProcessInstanceRecords = create()
                .delete(PROCESS_INSTANCE)
                .where(PROCESS_INSTANCE.END_TIME.isNotNull()
                        .and(PROCESS_INSTANCE.END_TIME.lt(new Date(System.currentTimeMillis() -
                                (PROCESS_INSTANCE_TIME.get() * 1000)))))
                .execute();

        long endProcessInstance = System.currentTimeMillis();
        Date expiredEvents = new Date(System.currentTimeMillis() - (EVENT_TIME.get() * 1000));
        long startServiceEvent = System.currentTimeMillis();
        int deletedServiceEventRecords = create()
                .delete(SERVICE_EVENT)
                .where(SERVICE_EVENT.CREATED.lt(expiredEvents)
                        .and(SERVICE_EVENT.STATE.eq(CommonStatesConstants.CREATED)))
                .execute();

        long endServiceEvent = System.currentTimeMillis();
        long startContainerEvent = System.currentTimeMillis();
        int deletedContainerEventRecords = create()
                .delete(CONTAINER_EVENT)
                .where(CONTAINER_EVENT.CREATED.lt(expiredEvents)
                .and(CONTAINER_EVENT.STATE.eq(CommonStatesConstants.CREATED)))
                .execute();
        long endContainerEvent = System.currentTimeMillis();

        long startAuditLog = System.currentTimeMillis();
        int deletedAuditLogRecords = create()
                .delete(AUDIT_LOG)
                .where(AUDIT_LOG.CREATED.lt(new Date(System.currentTimeMillis() - (AUDIT_LOG_TIME.get() * 1000))))
                .execute();
        long endAuditLog = System.currentTimeMillis();

        long startConfigItemStatusStart = System.currentTimeMillis();

        int deletedConfigItemStatuses = deleteConfigItemStatus(create().select(CONFIG_ITEM_STATUS.ID).from(CONFIG_ITEM_STATUS)
                .join(AGENT).on(CONFIG_ITEM_STATUS.AGENT_ID.eq(AGENT.ID)).where(AGENT.REMOVED.isNotNull()).limit(100));


        deletedConfigItemStatuses = deletedConfigItemStatuses + deleteConfigItemStatus(create().select(CONFIG_ITEM_STATUS.ID).from(CONFIG_ITEM_STATUS)
                .join(SERVICE).on(CONFIG_ITEM_STATUS.SERVICE_ID.eq(SERVICE.ID)).where(SERVICE.REMOVED.isNotNull()).limit(100));

        long endConfigItemStatusStart = System.currentTimeMillis();

        if ((deletedContainerEventRecords + deletedProcessInstanceRecords + deletedServiceEventRecords + deletedConfigItemStatuses) > 0) {
            logger.info("Deleted " + deletedProcessInstanceRecords + " from PROCESS_INSTANCE in " +
                    (endProcessInstance - startProcessInstance) + "ms, " +
                    deletedServiceEventRecords + " from SERVICE_EVENT in " + (endServiceEvent - startServiceEvent) +
                    "ms, and " +
                    deletedContainerEventRecords + " from CONTAINER_EVENT in " + (endContainerEvent - startContainerEvent) + "ms, " +
                    deletedConfigItemStatuses + " from CONFIG_ITEM_STATUS in " + (endConfigItemStatusStart - startConfigItemStatusStart) + "ms, " +
                    deletedAuditLogRecords + " from CONTAINER_EVENT in " + (endAuditLog - startAuditLog) + "ms. ");
        }
        logger.debug("Clean up for PROCESS_INSTANCE, SERVICE_EVENT, CONTAINER_EVENT, AUDIT_LOG, and CONFIG_ITEM_STATUS tables finished.");

    }

    private int deleteConfigItemStatus(ResultQuery<Record1<Long>>  query) {
        Result<Record1<Long>> toDelete;
        int deletedConfigItemStatuses = 0;
        while ( (toDelete = query.fetch()).size() > 0 ) {
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
