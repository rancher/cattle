package io.cattle.platform.core.cleanup;

import static io.cattle.platform.core.model.tables.ServiceEventTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.tables.AccountTable;
import io.cattle.platform.core.model.tables.AgentTable;
import io.cattle.platform.core.model.tables.AuditLogTable;
import io.cattle.platform.core.model.tables.AuthTokenTable;
import io.cattle.platform.core.model.tables.CertificateTable;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.core.model.tables.DeploymentUnitTable;
import io.cattle.platform.core.model.tables.DynamicSchemaTable;
import io.cattle.platform.core.model.tables.ExternalEventTable;
import io.cattle.platform.core.model.tables.GenericObjectTable;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.MachineDriverTable;
import io.cattle.platform.core.model.tables.MountTable;
import io.cattle.platform.core.model.tables.NetworkDriverTable;
import io.cattle.platform.core.model.tables.NetworkTable;
import io.cattle.platform.core.model.tables.ProcessExecutionTable;
import io.cattle.platform.core.model.tables.ProcessInstanceTable;
import io.cattle.platform.core.model.tables.ProjectMemberTable;
import io.cattle.platform.core.model.tables.ResourcePoolTable;
import io.cattle.platform.core.model.tables.ScheduledUpgradeTable;
import io.cattle.platform.core.model.tables.SecretTable;
import io.cattle.platform.core.model.tables.ServiceEventTable;
import io.cattle.platform.core.model.tables.ServiceLogTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.core.model.tables.StackTable;
import io.cattle.platform.core.model.tables.StorageDriverTable;
import io.cattle.platform.core.model.tables.StoragePoolHostMapTable;
import io.cattle.platform.core.model.tables.StoragePoolTable;
import io.cattle.platform.core.model.tables.SubnetTable;
import io.cattle.platform.core.model.tables.UserPreferenceTable;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.task.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.ResultQuery;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

/**
 * Programmatically delete purged database rows after they reach a configurable age.
 */
public class TableCleanup extends AbstractJooqDao implements Task {

    public static final Long SECOND_MILLIS = 1000L;

    private static final Logger log = LoggerFactory.getLogger(TableCleanup.class);

    public static final DynamicIntProperty QUERY_LIMIT_ROWS = ArchaiusUtil.getInt("cleanup.query_limit.rows");
    public static final DynamicLongProperty MAIN_TABLES_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("main_tables.purge.after.seconds");
    public static final DynamicLongProperty PROCESS_INSTANCE_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("process_instance.purge.after.seconds");
    public static final DynamicLongProperty EVENT_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("events.purge.after.seconds");
    public static final DynamicLongProperty AUDIT_LOG_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("audit_log.purge.after.seconds");
    public static final DynamicLongProperty SERVICE_LOG_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("service_log.purge.after.seconds");

    private List<CleanableTable> processInstanceTables;
    private List<CleanableTable> auditLogTables;
    private List<CleanableTable> serviceLogTables;
    private List<CleanableTable> otherTables;

    public TableCleanup(Configuration configuration) {
        super(configuration);
        this.processInstanceTables = getProcessInstanceTables();
        this.auditLogTables = getAuditLogTables();
        this.serviceLogTables = getServiceLogTables();
        this.otherTables = getOtherTables();
    }

    @Override
    public void run() {
        long current = new Date().getTime();

        Date processInstanceCutoff = new Date(current - PROCESS_INSTANCE_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("process_instance", processInstanceTables, processInstanceCutoff);

        Date eventTableCutoff = new Date(current - EVENT_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanupServiceEventTable(eventTableCutoff);

        Date auditLogCutoff = new Date(current - AUDIT_LOG_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("audit_log", auditLogTables, auditLogCutoff);

        Date serviceLogCutoff = new Date(current - SERVICE_LOG_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("service_log", serviceLogTables, serviceLogCutoff);

        Date otherCutoff = new Date(current - MAIN_TABLES_AGE_LIMIT_SECONDS.getValue() * SECOND_MILLIS);
        cleanup("other", otherTables, otherCutoff);
    }

    private void cleanupServiceEventTable(Date cutoff) {
        ResultQuery<Record1<Long>> ids = create()
                .select(SERVICE_EVENT.ID)
                .from(SERVICE_EVENT)
                .where(SERVICE_EVENT.CREATED.lt(cutoff))
                .and(SERVICE_EVENT.STATE.eq(CommonStatesConstants.CREATED))
                .limit(QUERY_LIMIT_ROWS.getValue());

        List<Long> toDelete = null;
        int rowsDeleted = 0;
        while ((toDelete = ids.fetch().into(Long.class)).size() > 0) {
            rowsDeleted += create().delete(SERVICE_EVENT)
            .where(SERVICE_EVENT.ID.in(toDelete)).execute();
        }

        if (rowsDeleted > 0) {
            log.info("[Rows Deleted] service_event={}", rowsDeleted);
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanup(String name, List<CleanableTable> tables, Date cutoffTime) {
        for (CleanableTable table : tables) {
            Field<Long> id = table.idField;
            Field<Date> remove = table.removeField;

            ResultQuery<Record1<Long>> ids = create()
                    .select(id)
                    .from(table.table)
                    .where(remove.lt(cutoffTime))
                    .limit(QUERY_LIMIT_ROWS.getValue());

            table.clearRowCounts();
            Result<Record1<Long>> toDelete;
            List<Long> idsToFix = new ArrayList<>();
            while ((toDelete = ids.fetch()).size() > 0) {
                List<Long> idsToDelete = new ArrayList<>();

                for (Record1<Long> record : toDelete) {
                    if (!idsToFix.contains(record.value1())) {
                        idsToDelete.add(record.value1());
                    }
                }

                if (idsToDelete.size() == 0) {
                    break;
                }

                List<ForeignKey<?, ?>> keys = getReferencesFrom(table, tables);
                for (ForeignKey<?, ?> key : keys) {
                    Table<?> referencingTable = key.getTable();
                    if (key.getFields().size() > 1) {
                        log.error("Composite foreign key filtering unsupported");
                    }
                    Field<Long> foreignKeyField = (Field<Long>) key.getFields().get(0);

                    ResultQuery<Record1<Long>> filterIds = create()
                        .selectDistinct(foreignKeyField)
                        .from(referencingTable)
                        .where(foreignKeyField.in(idsToDelete));

                    Result<Record1<Long>> toFilter = filterIds.fetch();
                    if (toFilter.size() > 0) {
                        for (Record1<Long> record : toFilter) {
                            if (idsToDelete.remove(record.value1())) {
                                idsToFix.add(record.value1());
                            }
                        }
                    }
                }

                try {
                    table.addRowsDeleted(create()
                            .delete(table.table)
                            .where(id.in(idsToDelete))
                            .execute());

                } catch (org.jooq.exception.DataAccessException e) {
                    log.info(e.getMessage());
                    break;
                }
            }
            if (idsToFix.size() > 0) {
                table.addRowsSkipped(idsToFix.size());
                log.info("Skipped {} where id in {}", table.table, idsToFix);
            }
        }
        StringBuffer buffDeleted = new StringBuffer("[Rows Deleted] ");
        StringBuffer buffSkipped = new StringBuffer("[Rows Skipped] ");
        boolean deletedActivity = false;
        boolean skippedActivity = false;
        for (CleanableTable table : tables) {
            if (table.getRowsDeleted() > 0) {
                buffDeleted.append(table.table.getName())
                    .append("=")
                    .append(table.getRowsDeleted())
                    .append(" ");
                deletedActivity = true;
            }
            if (table.getRowsSkipped() > 0) {
                buffSkipped.append(table.table.getName())
                    .append("=")
                    .append(table.getRowsSkipped())
                    .append(" ");
                skippedActivity = true;
            }
        }

        log.info("Cleanup {} tables [cutoff={}]", name, cutoffTime);
        if (deletedActivity) {
            log.info(buffDeleted.toString());
        }
        if (skippedActivity) {
            log.info(buffSkipped.toString());
        }
    }

    /**
     * Returns a list of foreign keys referencing a table
     *
     * @param table
     * @param others
     * @return
     */
    public static List<ForeignKey<?, ?>> getReferencesFrom(CleanableTable table, List<CleanableTable> others) {
        List<ForeignKey<?, ?>> keys = new ArrayList<>();
        for (CleanableTable other : others) {
            keys.addAll(table.table.getReferencesFrom(other.table));
        }
        return keys;
    }

    /**
     * Sorts a list of tables by their primary key references such that tables may be cleaned in an order
     * that doesn't violate any key constraints.
     *
     * @param tables The list of tables to sort
     */
    public static List<CleanableTable> sortByReferences(List<CleanableTable> tables) {
        List<CleanableTable> unsorted = new ArrayList<>(tables);
        List<CleanableTable> sorted = new ArrayList<>();

        int tableCount = unsorted.size();
        while (tableCount > 0) {
            for (int i = 0; i < unsorted.size(); i++) {
                CleanableTable table = unsorted.get(i);

                List<CleanableTable> others = new ArrayList<>(unsorted);
                others.remove(i);

                if (!JooqUtils.isReferencedBy(table.table, stripContext(others))) {
                    sorted.add(unsorted.remove(i--));
                }
            }

            if (tableCount == unsorted.size()) {
                log.error("Cycle detected in table references! Aborting. " + unsorted);
                System.exit(1);
            } else {
                tableCount = unsorted.size();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Table cleanup plan:");
            for (CleanableTable table : sorted) {
                log.debug(table.toString());
            }
        }

        return sorted;
    }

    private static List<Table<?>> stripContext(List<CleanableTable> cleanableTables) {
        List<Table<?>> tables = new ArrayList<>();
        for (CleanableTable cleanableTable : cleanableTables) {
            tables.add(cleanableTable.table);
        }
        return tables;
    }

    private static List<CleanableTable> getProcessInstanceTables() {
        List<CleanableTable> tables = Arrays.asList(
                CleanableTable.from(ProcessExecutionTable.PROCESS_EXECUTION),
                CleanableTable.from(ProcessInstanceTable.PROCESS_INSTANCE));
        return sortByReferences(tables);
    }

    private static List<CleanableTable> getAuditLogTables() {
        return Arrays.asList(CleanableTable.from(AuditLogTable.AUDIT_LOG));
    }

    private static List<CleanableTable> getServiceLogTables() {
        return Arrays.asList(CleanableTable.from(ServiceLogTable.SERVICE_LOG, ServiceLogTable.SERVICE_LOG.CREATED));
    }

    private static List<CleanableTable> getOtherTables() {
        List<CleanableTable> tables = Arrays.asList(
                CleanableTable.from(AccountTable.ACCOUNT),
                CleanableTable.from(AgentTable.AGENT),
                CleanableTable.from(AuthTokenTable.AUTH_TOKEN),
                CleanableTable.from(CertificateTable.CERTIFICATE),
                CleanableTable.from(CredentialTable.CREDENTIAL),
                CleanableTable.from(DeploymentUnitTable.DEPLOYMENT_UNIT),
                CleanableTable.from(DynamicSchemaTable.DYNAMIC_SCHEMA),
                CleanableTable.from(ExternalEventTable.EXTERNAL_EVENT),
                CleanableTable.from(GenericObjectTable.GENERIC_OBJECT),
                CleanableTable.from(HostTable.HOST),
                CleanableTable.from(InstanceTable.INSTANCE),
                CleanableTable.from(MachineDriverTable.MACHINE_DRIVER),
                CleanableTable.from(MountTable.MOUNT),
                CleanableTable.from(NetworkTable.NETWORK),
                CleanableTable.from(NetworkDriverTable.NETWORK_DRIVER),
                CleanableTable.from(ProjectMemberTable.PROJECT_MEMBER),
                CleanableTable.from(ResourcePoolTable.RESOURCE_POOL),
                CleanableTable.from(ServiceTable.SERVICE),
                CleanableTable.from(StackTable.STACK),
                CleanableTable.from(StorageDriverTable.STORAGE_DRIVER),
                CleanableTable.from(StoragePoolTable.STORAGE_POOL),
                CleanableTable.from(StoragePoolHostMapTable.STORAGE_POOL_HOST_MAP),
                CleanableTable.from(SubnetTable.SUBNET),
                CleanableTable.from(UserPreferenceTable.USER_PREFERENCE),
                CleanableTable.from(VolumeTable.VOLUME),
                // These tables are cleaned through specialized logic but we need to keep them in the "other" list so that they
                // are picked up for foreign key references.
                CleanableTable.from(ServiceEventTable.SERVICE_EVENT),
                CleanableTable.from(ScheduledUpgradeTable.SCHEDULED_UPGRADE),
                CleanableTable.from(SecretTable.SECRET));
        /* The most offending tables never set remove_time
        service_event
        external_handler_external_handler_process_map
        instance_label_map
        mount
        instance_link
        */
        return sortByReferences(tables);
    }

    @Override
    public String getName() {
        return "table.cleanup";
    }

}
