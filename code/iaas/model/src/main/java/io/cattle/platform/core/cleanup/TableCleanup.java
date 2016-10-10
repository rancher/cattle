package io.cattle.platform.core.cleanup;

import java.util.ArrayList;
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

import io.cattle.platform.core.model.tables.*;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.jooq.utils.JooqUtils;

/**
 * Programmatically delete purged database rows after they reach a configurable age.
 * 
 * @author joliver
 *
 */
public class TableCleanup extends AbstractJooqDao {

	private static final Logger log = LoggerFactory.getLogger(TableCleanup.class);

	private static final DynamicIntProperty QUERY_LIMIT_ROWS = 
			DynamicPropertyFactory.getInstance().getIntProperty("cleanup.query_limit.rows", 100);
	
	private static final DynamicLongProperty AGE_LIMIT_HOURS =
			DynamicPropertyFactory.getInstance().getLongProperty("cleanup.age_limit.hours", 168);
	
	private List<CleanableTable> cleanableTables;

	public TableCleanup() {
		cleanableTables = getCleanableTables();
		orderByReferences(cleanableTables);
	}

	public void cleanup() {
		Date currentTime = new Date();
		Date cutoffTime = new Date(currentTime.getTime() - AGE_LIMIT_HOURS.getValue() * 60 * 60 * 1000);
		cleanup(cutoffTime);
	}

	private void cleanup(Date cutoffTime) {
		log.info("Table cleanup started (cutoff=" + cutoffTime + ")");
		
		for (CleanableTable cleanableTable : cleanableTables) {
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
				log.debug(table.getName() + ": " + idsToDelete.size() + " to delete");
				
				try {
					deletedRows += create()
							.delete(table)
							.where(id.in(idsToDelete))
							.execute();
					
				} catch (org.jooq.exception.DataAccessException e) {
					log.error(e.getMessage());
					break;
				}

				log.info("Deleted " + deletedRows + " rows from " + table);
			}
		}
		log.info("Table cleanup completed");
	}

	protected static void orderByReferences(List<CleanableTable> tables) {
		List<CleanableTable> orderedTables = new ArrayList<CleanableTable>();

		int tableCount = tables.size();
		while (tableCount > 0) {
			for (int i = 0; i < tables.size(); i++) {
				CleanableTable table = tables.get(i);

				List<CleanableTable> others = new ArrayList<CleanableTable>(tables);
				others.remove(i);

				if (!JooqUtils.isReferencedBy(table.table, stripContext(others))) {
					orderedTables.add(tables.remove(i--));
				}
			}

			if (tableCount == tables.size()) {
				log.error("Cycle detected in table references! Aborting.");
				System.exit(1);
			} else {
				tableCount = tables.size();
			}
		}

		log.debug("Table cleanup plan:");
		for (CleanableTable table : orderedTables) {
			log.debug(table.toString());
			tables.add(table);
		}
	}
	
	private static List<Table<?>> stripContext(List<CleanableTable> cleanableTables) {
		List<Table<?>> tables = new ArrayList<Table<?>>();
		for (CleanableTable cleanableTable : cleanableTables) {
			tables.add(cleanableTable.table);
		}
		return tables;
	}

	protected static List<CleanableTable> getCleanableTables() {
		List<CleanableTable> tables = new ArrayList<CleanableTable>();
		tables.add(CleanableTable.from(AccountTable.ACCOUNT));
		tables.add(CleanableTable.from(AgentTable.AGENT));
		tables.add(CleanableTable.from(AgentGroupTable.AGENT_GROUP));
		tables.add(CleanableTable.from(AuditLogTable.AUDIT_LOG));
		tables.add(CleanableTable.from(AuthTokenTable.AUTH_TOKEN));
		tables.add(CleanableTable.from(BackupTable.BACKUP));
		tables.add(CleanableTable.from(BackupTargetTable.BACKUP_TARGET));
		tables.add(CleanableTable.from(CertificateTable.CERTIFICATE));
		tables.add(CleanableTable.from(ClusterHostMapTable.CLUSTER_HOST_MAP));
		tables.add(CleanableTable.from(ConfigItemStatusTable.CONFIG_ITEM_STATUS));
		tables.add(CleanableTable.from(ContainerEventTable.CONTAINER_EVENT));
		tables.add(CleanableTable.from(CredentialTable.CREDENTIAL));
		tables.add(CleanableTable.from(CredentialInstanceMapTable.CREDENTIAL_INSTANCE_MAP));
		tables.add(CleanableTable.from(DeploymentUnitTable.DEPLOYMENT_UNIT));
		tables.add(CleanableTable.from(DynamicSchemaTable.DYNAMIC_SCHEMA));
		tables.add(CleanableTable.from(ExternalEventTable.EXTERNAL_EVENT));
		tables.add(CleanableTable.from(ExternalHandlerTable.EXTERNAL_HANDLER));
		tables.add(CleanableTable.from(ExternalHandlerExternalHandlerProcessMapTable.EXTERNAL_HANDLER_EXTERNAL_HANDLER_PROCESS_MAP));
		tables.add(CleanableTable.from(ExternalHandlerProcessTable.EXTERNAL_HANDLER_PROCESS));
		tables.add(CleanableTable.from(GenericObjectTable.GENERIC_OBJECT));
		tables.add(CleanableTable.from(HealthcheckInstanceTable.HEALTHCHECK_INSTANCE));
		tables.add(CleanableTable.from(HealthcheckInstanceHostMapTable.HEALTHCHECK_INSTANCE_HOST_MAP));
		tables.add(CleanableTable.from(HostTable.HOST));
		tables.add(CleanableTable.from(HostIpAddressMapTable.HOST_IP_ADDRESS_MAP));
		tables.add(CleanableTable.from(HostLabelMapTable.HOST_LABEL_MAP));
		tables.add(CleanableTable.from(HostVnetMapTable.HOST_VNET_MAP));
		tables.add(CleanableTable.from(ImageTable.IMAGE));
		tables.add(CleanableTable.from(ImageStoragePoolMapTable.IMAGE_STORAGE_POOL_MAP));
		tables.add(CleanableTable.from(InstanceTable.INSTANCE));
		tables.add(CleanableTable.from(InstanceHostMapTable.INSTANCE_HOST_MAP));
		tables.add(CleanableTable.from(InstanceLabelMapTable.INSTANCE_LABEL_MAP));
		tables.add(CleanableTable.from(InstanceLinkTable.INSTANCE_LINK));
		tables.add(CleanableTable.from(IpAddressTable.IP_ADDRESS));
		tables.add(CleanableTable.from(IpAddressNicMapTable.IP_ADDRESS_NIC_MAP));
		tables.add(CleanableTable.from(IpAssociationTable.IP_ASSOCIATION));
		tables.add(CleanableTable.from(IpPoolTable.IP_POOL));
		tables.add(CleanableTable.from(LabelTable.LABEL));
		tables.add(CleanableTable.from(MachineDriverTable.MACHINE_DRIVER));
		tables.add(CleanableTable.from(MountTable.MOUNT));
		tables.add(CleanableTable.from(NetworkTable.NETWORK));
		tables.add(CleanableTable.from(NetworkServiceTable.NETWORK_SERVICE));
		tables.add(CleanableTable.from(NetworkServiceProviderTable.NETWORK_SERVICE_PROVIDER));
		tables.add(CleanableTable.from(NetworkServiceProviderInstanceMapTable.NETWORK_SERVICE_PROVIDER_INSTANCE_MAP));
		tables.add(CleanableTable.from(NicTable.NIC));
		tables.add(CleanableTable.from(OfferingTable.OFFERING));
		tables.add(CleanableTable.from(PhysicalHostTable.PHYSICAL_HOST));
		tables.add(CleanableTable.from(PortTable.PORT));
		tables.add(CleanableTable.from(ProcessExecutionTable.PROCESS_EXECUTION));
		tables.add(CleanableTable.from(ProcessInstanceTable.PROCESS_INSTANCE));
		tables.add(CleanableTable.from(ProjectMemberTable.PROJECT_MEMBER));
		tables.add(CleanableTable.from(ResourcePoolTable.RESOURCE_POOL));
		tables.add(CleanableTable.from(ServiceTable.SERVICE));
		tables.add(CleanableTable.from(ServiceConsumeMapTable.SERVICE_CONSUME_MAP));
		tables.add(CleanableTable.from(ServiceEventTable.SERVICE_EVENT));
		tables.add(CleanableTable.from(ServiceExposeMapTable.SERVICE_EXPOSE_MAP));
		tables.add(CleanableTable.from(ServiceIndexTable.SERVICE_INDEX));
		tables.add(CleanableTable.from(ServiceLogTable.SERVICE_LOG));
		tables.add(CleanableTable.from(SnapshotTable.SNAPSHOT));
		tables.add(CleanableTable.from(StackTable.STACK));
		tables.add(CleanableTable.from(StoragePoolTable.STORAGE_POOL));
		tables.add(CleanableTable.from(StoragePoolHostMapTable.STORAGE_POOL_HOST_MAP));
		tables.add(CleanableTable.from(SubnetTable.SUBNET));
		tables.add(CleanableTable.from(SubnetVnetMapTable.SUBNET_VNET_MAP));
		tables.add(CleanableTable.from(TaskInstanceTable.TASK_INSTANCE));
		tables.add(CleanableTable.from(UserPreferenceTable.USER_PREFERENCE));
		tables.add(CleanableTable.from(VnetTable.VNET));
		tables.add(CleanableTable.from(VolumeTable.VOLUME));
		tables.add(CleanableTable.from(VolumeStoragePoolMapTable.VOLUME_STORAGE_POOL_MAP));
		tables.add(CleanableTable.from(ZoneTable.ZONE));
		return tables;
	}

}
