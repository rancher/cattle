package io.cattle.platform.core.cleanup;

import io.cattle.platform.core.model.tables.*;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.jooq.utils.JooqUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.ResultQuery;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;

/**
 * Programmatically delete purged database rows after they reach a configurable age.
 * 
 * @author joliver
 *
 */
public class TableCleanup extends AbstractJooqDao {

    private static final Logger log = LoggerFactory.getLogger(TableCleanup.class);

    public static final DynamicIntProperty QUERY_LIMIT_ROWS = 
            DynamicPropertyFactory.getInstance().getIntProperty("cleanup.query_limit.rows", 100);
    
    public static final DynamicLongProperty DEFAULT_AGE_LIMIT_HOURS =
            DynamicPropertyFactory.getInstance().getLongProperty("cleanup.age_limit.hours", 168);
    
    public static final DynamicLongProperty PROCESS_INSTANCE_AGE_LIMIT_SECONDS =
            DynamicPropertyFactory.getInstance().getLongProperty(
                    "process_instance.purge.after.seconds", DEFAULT_AGE_LIMIT_HOURS.get());

    public static final DynamicLongProperty EVENT_AGE_LIMIT_SECONDS =
            DynamicPropertyFactory.getInstance().getLongProperty(
                    "events.purge.after.seconds", DEFAULT_AGE_LIMIT_HOURS.get());

    public static final DynamicLongProperty AUDIT_LOG_AGE_LIMIT_SECONDS =
            DynamicPropertyFactory.getInstance().getLongProperty(
                    "audit_log.purge.after.seconds", DEFAULT_AGE_LIMIT_HOURS.get());

    public static final DynamicLongProperty SERVICE_LOG_AGE_LIMIT_SECONDS =
            DynamicPropertyFactory.getInstance().getLongProperty(
                    "service_log.purge.after.seconds", DEFAULT_AGE_LIMIT_HOURS.get());
    
    public static final Long SECOND_MILLIS = 1000L;
    public static final Long HOUR_MILLIS = 60 * 60 * SECOND_MILLIS;

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
        
        Date otherCutoff = new Date(current - DEFAULT_AGE_LIMIT_HOURS.getValue() * HOUR_MILLIS);
        cleanup("other", otherTables, otherCutoff);
    }

    private void cleanup(String name, List<CleanableTable> tables, Date cutoffTime) {
        log.info("Cleanup {} tables started (cutoff={})", name, cutoffTime);
        
        for (CleanableTable cleanableTable : tables) {
            Table<?> table = cleanableTable.table;
            Field<Long> id = cleanableTable.idField;
            Field<Date> remove = cleanableTable.removeField;
            
            ResultQuery<Record1<Long>> ids = create()
                    .select(id)
                    .from(table)
                    .where(remove.lt(cutoffTime))
                    .limit(QUERY_LIMIT_ROWS.getValue());

            int deletedRows = 0;
            Result<Record1<Long>> toDelete;
            while ((toDelete = ids.fetch()).size() > 0) {
                List<Long> idsToDelete = new ArrayList<>();

                for (Record1<Long> record : toDelete) {
                    idsToDelete.add(record.value1());
                }
                
                try {
                    deletedRows += create()
                            .delete(table)
                            .where(id.in(idsToDelete))
                            .execute();
                    
                } catch (org.jooq.exception.DataAccessException e) {
                    log.error(e.getMessage());
                    break;
                }
            }
            log.info("Deleted " + deletedRows + " rows from " + table);
        }
        log.info("Cleanup {} tables completed", name);
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
                CleanableTable.from(AgentGroupTable.AGENT_GROUP),
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
                CleanableTable.from(HostVnetMapTable.HOST_VNET_MAP),
                CleanableTable.from(ImageTable.IMAGE),
                CleanableTable.from(ImageStoragePoolMapTable.IMAGE_STORAGE_POOL_MAP),
                CleanableTable.from(InstanceTable.INSTANCE),
                CleanableTable.from(InstanceHostMapTable.INSTANCE_HOST_MAP),
                CleanableTable.from(InstanceLabelMapTable.INSTANCE_LABEL_MAP),
                CleanableTable.from(InstanceLinkTable.INSTANCE_LINK),
                CleanableTable.from(IpAddressTable.IP_ADDRESS),
                CleanableTable.from(IpAddressNicMapTable.IP_ADDRESS_NIC_MAP),
                CleanableTable.from(IpAssociationTable.IP_ASSOCIATION),
                CleanableTable.from(IpPoolTable.IP_POOL),
                CleanableTable.from(LabelTable.LABEL),
                CleanableTable.from(MachineDriverTable.MACHINE_DRIVER),
                CleanableTable.from(MountTable.MOUNT),
                CleanableTable.from(NetworkTable.NETWORK),
                CleanableTable.from(NetworkServiceTable.NETWORK_SERVICE),
                CleanableTable.from(NetworkServiceProviderTable.NETWORK_SERVICE_PROVIDER),
                CleanableTable.from(NetworkServiceProviderInstanceMapTable.NETWORK_SERVICE_PROVIDER_INSTANCE_MAP),
                CleanableTable.from(NicTable.NIC),
                CleanableTable.from(OfferingTable.OFFERING),
                CleanableTable.from(PhysicalHostTable.PHYSICAL_HOST),
                CleanableTable.from(PortTable.PORT),
                CleanableTable.from(ProjectMemberTable.PROJECT_MEMBER),
                CleanableTable.from(ResourcePoolTable.RESOURCE_POOL),
                CleanableTable.from(ServiceTable.SERVICE),
                CleanableTable.from(ServiceConsumeMapTable.SERVICE_CONSUME_MAP),
                CleanableTable.from(ServiceExposeMapTable.SERVICE_EXPOSE_MAP),
                CleanableTable.from(ServiceIndexTable.SERVICE_INDEX),
                CleanableTable.from(ServiceLogTable.SERVICE_LOG),
                CleanableTable.from(SnapshotTable.SNAPSHOT),
                CleanableTable.from(StackTable.STACK),
                CleanableTable.from(StoragePoolTable.STORAGE_POOL),
                CleanableTable.from(StoragePoolHostMapTable.STORAGE_POOL_HOST_MAP),
                CleanableTable.from(SubnetTable.SUBNET),
                CleanableTable.from(SubnetVnetMapTable.SUBNET_VNET_MAP),
                CleanableTable.from(TaskInstanceTable.TASK_INSTANCE),
                CleanableTable.from(UserPreferenceTable.USER_PREFERENCE),
                CleanableTable.from(VnetTable.VNET),
                CleanableTable.from(VolumeTable.VOLUME),
                CleanableTable.from(VolumeStoragePoolMapTable.VOLUME_STORAGE_POOL_MAP),
                CleanableTable.from(ZoneTable.ZONE));
        return sortByReferences(tables);
    }

}
