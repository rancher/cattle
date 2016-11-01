package io.cattle.platform.core.cleanup;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.tables.AccountTable;
import io.cattle.platform.core.model.tables.AgentTable;
import io.cattle.platform.core.model.tables.AuditLogTable;
import io.cattle.platform.core.model.tables.AuthTokenTable;
import io.cattle.platform.core.model.tables.BackupTable;
import io.cattle.platform.core.model.tables.BackupTargetTable;
import io.cattle.platform.core.model.tables.CertificateTable;
import io.cattle.platform.core.model.tables.ClusterHostMapTable;
import io.cattle.platform.core.model.tables.ConfigItemStatusTable;
import io.cattle.platform.core.model.tables.ContainerEventTable;
import io.cattle.platform.core.model.tables.CredentialInstanceMapTable;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.core.model.tables.DeploymentUnitTable;
import io.cattle.platform.core.model.tables.DynamicSchemaTable;
import io.cattle.platform.core.model.tables.ExternalEventTable;
import io.cattle.platform.core.model.tables.ExternalHandlerExternalHandlerProcessMapTable;
import io.cattle.platform.core.model.tables.ExternalHandlerProcessTable;
import io.cattle.platform.core.model.tables.ExternalHandlerTable;
import io.cattle.platform.core.model.tables.GenericObjectTable;
import io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable;
import io.cattle.platform.core.model.tables.HealthcheckInstanceTable;
import io.cattle.platform.core.model.tables.HostIpAddressMapTable;
import io.cattle.platform.core.model.tables.HostLabelMapTable;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.ImageStoragePoolMapTable;
import io.cattle.platform.core.model.tables.ImageTable;
import io.cattle.platform.core.model.tables.InstanceHostMapTable;
import io.cattle.platform.core.model.tables.InstanceLabelMapTable;
import io.cattle.platform.core.model.tables.InstanceLinkTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressNicMapTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.LabelTable;
import io.cattle.platform.core.model.tables.MachineDriverTable;
import io.cattle.platform.core.model.tables.MountTable;
import io.cattle.platform.core.model.tables.NetworkTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.PhysicalHostTable;
import io.cattle.platform.core.model.tables.PortTable;
import io.cattle.platform.core.model.tables.ProcessExecutionTable;
import io.cattle.platform.core.model.tables.ProcessInstanceTable;
import io.cattle.platform.core.model.tables.ProjectMemberTable;
import io.cattle.platform.core.model.tables.ResourcePoolTable;
import io.cattle.platform.core.model.tables.ServiceConsumeMapTable;
import io.cattle.platform.core.model.tables.ServiceEventTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceIndexTable;
import io.cattle.platform.core.model.tables.ServiceLogTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.core.model.tables.SnapshotTable;
import io.cattle.platform.core.model.tables.StackTable;
import io.cattle.platform.core.model.tables.StoragePoolHostMapTable;
import io.cattle.platform.core.model.tables.StoragePoolTable;
import io.cattle.platform.core.model.tables.SubnetTable;
import io.cattle.platform.core.model.tables.TaskInstanceTable;
import io.cattle.platform.core.model.tables.UserPreferenceTable;
import io.cattle.platform.core.model.tables.VolumeStoragePoolMapTable;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.core.model.tables.ZoneTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.jooq.utils.JooqUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
 *
 * @author joliver
 *
 */
public class TableCleanup extends AbstractJooqDao {

    public static final Long SECOND_MILLIS = 1000L;

    private static final Logger log = LoggerFactory.getLogger(TableCleanup.class);

    public static final DynamicIntProperty QUERY_LIMIT_ROWS = ArchaiusUtil.getInt("cleanup.query_limit.rows");
    public static final DynamicLongProperty MAIN_TABLES_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("main_tables.purge.after.seconds");
    public static final DynamicLongProperty PROCESS_INSTANCE_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("process_instance.purge.after.seconds");
    public static final DynamicLongProperty EVENT_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("events.purge.after.seconds");
    public static final DynamicLongProperty AUDIT_LOG_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("audit_log.purge.after.seconds");
    public static final DynamicLongProperty SERVICE_LOG_AGE_LIMIT_SECONDS = ArchaiusUtil.getLong("service_log.purge.after.seconds");

    private List<CleanableTable> processInstanceTables;
    private List<CleanableTable> eventTables;
    private List<CleanableTable> auditLogTables;
    private List<CleanableTable> serviceLogTables;
    private List<CleanableTable> otherTables;

    public TableCleanup() {
        this.processInstanceTables = getProcessInstanceTables();
        this.eventTables = getEventTables();
        this.auditLogTables = getAuditLogTables();
        this.serviceLogTables = getServiceLogTables();
        this.otherTables = getOtherTables();
    }

    public void cleanup() {
        long current = new Date().getTime();

        Date processInstanceCutoff = new Date(current - PROCESS_INSTANCE_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("process_instance", processInstanceTables, processInstanceCutoff);

        Date eventTableCutoff = new Date(current - EVENT_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("event", eventTables, eventTableCutoff);

        Date auditLogCutoff = new Date(current - AUDIT_LOG_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("audit_log", auditLogTables, auditLogCutoff);

        Date serviceLogCutoff = new Date(current - SERVICE_LOG_AGE_LIMIT_SECONDS.get() * SECOND_MILLIS);
        cleanup("service_log", serviceLogTables, serviceLogCutoff);

        Date otherCutoff = new Date(current - MAIN_TABLES_AGE_LIMIT_SECONDS.getValue() * SECOND_MILLIS);
        cleanup("other", otherTables, otherCutoff);
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

                for (ForeignKey<?, ?> key : getReferencesFrom(table, tables)) {
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
                    log.error(e.getMessage());
                    break;
                }
            }
            if (idsToFix.size() > 0) {
                table.addRowsSkipped(idsToFix.size());
                if (log.isDebugEnabled()) {
                    log.debug("Skipped {} where id in {}", table.table, idsToFix);
                }
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
            log.warn(buffSkipped.toString());
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
        List<ForeignKey<?, ?>> keys = new ArrayList<ForeignKey<?, ?>>();
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
        List<CleanableTable> unsorted = new ArrayList<CleanableTable>(tables);
        List<CleanableTable> sorted = new ArrayList<CleanableTable>();

        int tableCount = unsorted.size();
        while (tableCount > 0) {
            for (int i = 0; i < unsorted.size(); i++) {
                CleanableTable table = unsorted.get(i);

                List<CleanableTable> others = new ArrayList<CleanableTable>(unsorted);
                others.remove(i);

                if (!JooqUtils.isReferencedBy(table.table, stripContext(others))) {
                    sorted.add(unsorted.remove(i--));
                }
            }

            if (tableCount == unsorted.size()) {
                log.error("Cycle detected in table references! Aborting.");
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
        List<Table<?>> tables = new ArrayList<Table<?>>();
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

    private static List<CleanableTable> getEventTables() {
        List<CleanableTable> tables = Arrays.asList(
                CleanableTable.from(ContainerEventTable.CONTAINER_EVENT),
                CleanableTable.from(ServiceEventTable.SERVICE_EVENT));
        return sortByReferences(tables);
    }

    private static List<CleanableTable> getAuditLogTables() {
        return Arrays.asList(CleanableTable.from(AuditLogTable.AUDIT_LOG));
    }

    private static List<CleanableTable> getServiceLogTables() {
        return Arrays.asList(CleanableTable.from(ServiceLogTable.SERVICE_LOG));
    }

    private static List<CleanableTable> getOtherTables() {
        List<CleanableTable> tables = Arrays.asList(
                CleanableTable.from(AccountTable.ACCOUNT),
                CleanableTable.from(AgentTable.AGENT),
                CleanableTable.from(AuthTokenTable.AUTH_TOKEN),
                CleanableTable.from(BackupTable.BACKUP),
                CleanableTable.from(BackupTargetTable.BACKUP_TARGET),
                CleanableTable.from(CertificateTable.CERTIFICATE),
                CleanableTable.from(ClusterHostMapTable.CLUSTER_HOST_MAP),
                CleanableTable.from(ConfigItemStatusTable.CONFIG_ITEM_STATUS),
                CleanableTable.from(CredentialTable.CREDENTIAL),
                CleanableTable.from(CredentialInstanceMapTable.CREDENTIAL_INSTANCE_MAP),
                CleanableTable.from(DeploymentUnitTable.DEPLOYMENT_UNIT),
                CleanableTable.from(DynamicSchemaTable.DYNAMIC_SCHEMA),
                CleanableTable.from(ExternalEventTable.EXTERNAL_EVENT),
                CleanableTable.from(ExternalHandlerTable.EXTERNAL_HANDLER),
                CleanableTable.from(ExternalHandlerExternalHandlerProcessMapTable.EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP),
                CleanableTable.from(ExternalHandlerProcessTable.EXTERNAL_HANDLER_PROCESS),
                CleanableTable.from(GenericObjectTable.GENERIC_OBJECT),
                CleanableTable.from(HealthcheckInstanceTable.HEALTHCHECK_INSTANCE),
                CleanableTable.from(HealthcheckInstanceHostMapTable.HEALTHCHECK_INSTANCE_HOST_MAP),
                CleanableTable.from(HostTable.HOST),
                CleanableTable.from(HostIpAddressMapTable.HOST_IP_ADDRESS_MAP),
                CleanableTable.from(HostLabelMapTable.HOST_LABEL_MAP),
                CleanableTable.from(ImageTable.IMAGE),
                CleanableTable.from(ImageStoragePoolMapTable.IMAGE_STORAGE_POOL_MAP),
                CleanableTable.from(InstanceTable.INSTANCE),
                CleanableTable.from(InstanceHostMapTable.INSTANCE_HOST_MAP),
                CleanableTable.from(InstanceLabelMapTable.INSTANCE_LABEL_MAP),
                CleanableTable.from(InstanceLinkTable.INSTANCE_LINK),
                CleanableTable.from(IpAddressTable.IP_ADDRESS),
                CleanableTable.from(IpAddressNicMapTable.IP_ADDRESS_NIC_MAP),
                CleanableTable.from(LabelTable.LABEL),
                CleanableTable.from(MachineDriverTable.MACHINE_DRIVER),
                CleanableTable.from(MountTable.MOUNT),
                CleanableTable.from(NetworkTable.NETWORK),
                CleanableTable.from(NicTable.NIC),
                CleanableTable.from(PhysicalHostTable.PHYSICAL_HOST),
                CleanableTable.from(PortTable.PORT),
                CleanableTable.from(ProjectMemberTable.PROJECT_MEMBER),
                CleanableTable.from(ResourcePoolTable.RESOURCE_POOL),
                CleanableTable.from(ServiceTable.SERVICE),
                CleanableTable.from(ServiceConsumeMapTable.SERVICE_CONSUME_MAP),
                // we let users clean this table up with a different cutoffTime but it references healthcheck_instance so we
                // have to re-clean it with the same cutoffTime as the main tables to best-effort prevent FK violations...
                CleanableTable.from(ServiceEventTable.SERVICE_EVENT),
                CleanableTable.from(ServiceExposeMapTable.SERVICE_EXPOSE_MAP),
                CleanableTable.from(ServiceIndexTable.SERVICE_INDEX),
                CleanableTable.from(ServiceLogTable.SERVICE_LOG),
                CleanableTable.from(SnapshotTable.SNAPSHOT),
                CleanableTable.from(StackTable.STACK),
                CleanableTable.from(StoragePoolTable.STORAGE_POOL),
                CleanableTable.from(StoragePoolHostMapTable.STORAGE_POOL_HOST_MAP),
                CleanableTable.from(SubnetTable.SUBNET),
                CleanableTable.from(TaskInstanceTable.TASK_INSTANCE),
                CleanableTable.from(UserPreferenceTable.USER_PREFERENCE),
                CleanableTable.from(VolumeTable.VOLUME),
                CleanableTable.from(VolumeStoragePoolMapTable.VOLUME_STORAGE_POOL_MAP),
                CleanableTable.from(ZoneTable.ZONE));
        /* The most offending tables never set remove_time
        service_event
        external_handler_external_handler_process_map
        instance_label_map
        mount
        instance_link
        */
        return sortByReferences(tables);
    }

}
