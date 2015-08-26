package io.cattle.platform.core.cleanup;

import static io.cattle.platform.core.model.tables.ContainerEventTable.CONTAINER_EVENT;
import static io.cattle.platform.core.model.tables.ProcessInstanceTable.PROCESS_INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceEventTable.SERVICE_EVENT;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.netflix.config.DynamicLongProperty;


public class TableCleanUp extends AbstractJooqDao {

    private static final Log logger = LogFactory.getLog(TableCleanUp.class);
    private static final DynamicLongProperty PROCESS_INSTANCE_TIME = ArchaiusUtil.getLong("process_instance.purge.after.seconds");
    private static final DynamicLongProperty EVENT_TIME = ArchaiusUtil.getLong("events.purge.after.seconds");


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
        if ((deletedContainerEventRecords + deletedProcessInstanceRecords + deletedServiceEventRecords) > 0) {
            logger.info("Deleted " + deletedProcessInstanceRecords + " from PROCESS_INSTANCE in " +
                    (endProcessInstance - startProcessInstance) + "ms, " +
                    deletedServiceEventRecords + " from SERVICE_EVENT in " + (endServiceEvent - startServiceEvent) +
                    "ms, and " +
                    deletedContainerEventRecords + " from CONTAINER_EVENT in " + (endContainerEvent - startContainerEvent) + "ms.");
        }
        logger.debug("Clean up for PROCESS_INSTANCE table, SERVICE_EVENT table, and CONTAINER_EVENT table finished.");

    }
}
