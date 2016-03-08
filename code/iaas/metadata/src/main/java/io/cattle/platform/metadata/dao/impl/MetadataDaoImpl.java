package io.cattle.platform.metadata.dao.impl;

import static io.cattle.platform.core.model.tables.CredentialInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.IpAssociationTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import static io.cattle.platform.core.model.tables.VnetTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Offering;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.Zone;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NetworkTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.SubnetTable;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.core.model.tables.records.VnetRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.AggregateMultiRecordMapper;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.metadata.dao.MetadataDao;
import io.cattle.platform.metadata.data.MetadataEntry;
import io.cattle.platform.metadata.data.MetadataRedirectData;
import io.cattle.platform.object.ObjectManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.ResultQuery;

public class MetadataDaoImpl extends AbstractJooqDao implements MetadataDao {

    NetworkDao networkDao;
    ObjectManager objectManager;

    private List<MetadataEntry> getMetaDataInternal(Instance agentInstance, boolean singleInstance) {
        Condition condition = null;

        MultiRecordMapper<MetadataEntry> mapper = new AggregateMultiRecordMapper<MetadataEntry>(MetadataEntry.class);

        InstanceTable instance = mapper.add(INSTANCE);
        NicTable nic = mapper.add(NIC);
        IpAddressTable primaryIp = mapper.add(IP_ADDRESS);
        IpAddressTable publicIp = mapper.add(IP_ADDRESS);
        VolumeTable volume = mapper.add(VOLUME);
        CredentialTable credential = mapper.add(CREDENTIAL);
        NetworkTable network = mapper.add(NETWORK);
        SubnetTable subnet = mapper.add(SUBNET);

        if ( singleInstance ) {
            condition = nic.INSTANCE_ID.eq(agentInstance.getId());
        } else {
            List<? extends NetworkService> services = networkDao.getAgentInstanceNetworkService(agentInstance.getId(), NetworkServiceConstants.KIND_METADATA);

            if ( services.size() == 0 ) {
                return Collections.emptyList();
            }

            Record any = create()
                    .select(VNET.fields())
                    .from(VNET)
                    .join(NIC)
                        .on(NIC.VNET_ID.eq(VNET.ID))
                    .where(VNET.NETWORK_ID.eq(services.get(0).getNetworkId())
                            .and(NIC.INSTANCE_ID.eq(agentInstance.getId())))
                    .fetchAny();

            if ( any.size() == 0 ) {
                return Collections.emptyList();
            }

            Vnet vnet = any.into(VnetRecord.class);

            condition = nic.VNET_ID.eq(vnet.getId());
        }


        ResultQuery<?> q = create()
            .select(mapper.fields())
            .from(nic)
            .join(instance)
                .on(nic.INSTANCE_ID.eq(instance.ID))
            .join(network)
                .on(network.ID.eq(nic.NETWORK_ID))
            .leftOuterJoin(volume)
                .on(volume.INSTANCE_ID.eq(instance.ID)
                    .and(volume.REMOVED.isNull()))
            .leftOuterJoin(CREDENTIAL_INSTANCE_MAP)
                .on(CREDENTIAL_INSTANCE_MAP.INSTANCE_ID.eq(instance.ID)
                        .and(CREDENTIAL_INSTANCE_MAP.REMOVED.isNull()))
            .leftOuterJoin(credential)
                .on(CREDENTIAL_INSTANCE_MAP.CREDENTIAL_ID.eq(credential.ID)
                    .and(credential.REMOVED.isNull())
                    .and(credential.KIND.eq(CredentialConstants.KIND_SSH_KEY)))
            .leftOuterJoin(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.ID)
                    .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull()))
            .leftOuterJoin(primaryIp)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(primaryIp.ID)
                    .and(primaryIp.ROLE.in(IpAddressConstants.ROLE_PRIMARY, IpAddressConstants.ROLE_SECONDARY))
                    .and(primaryIp.REMOVED.isNull()))
            .leftOuterJoin(subnet)
                .on(primaryIp.SUBNET_ID.eq(subnet.ID))
            .leftOuterJoin(IP_ASSOCIATION)
                .on(IP_ASSOCIATION.CHILD_IP_ADDRESS_ID.eq(primaryIp.ID)
                    .and(IP_ASSOCIATION.REMOVED.isNull()))
            .leftOuterJoin(publicIp)
                .on(IP_ASSOCIATION.IP_ADDRESS_ID.eq(publicIp.ID)
                    .and(publicIp.ROLE.eq(IpAddressConstants.ROLE_PUBLIC))
                    .and(publicIp.REMOVED.isNull()))
            .where(condition.and(nic.REMOVED.isNull()));

        return q.fetch().map(mapper);
    }

    @Override
    public List<MetadataEntry> getMetadata(Instance agentInstance) {
        return getMetaDataInternal(agentInstance, false);
    }

    @Override
    public MetadataEntry getMetadataForInstance(Instance agentInstance) {
        List<MetadataEntry> entries = getMetaDataInternal(agentInstance, true);

        return entries.size() > 0 ? entries.get(0) : null;
    }

    @Override
    public List<MetadataRedirectData> getMetadataRedirects(Agent agent) {
        AggregateMultiRecordMapper<MetadataRedirectData> mapper =
                new AggregateMultiRecordMapper<MetadataRedirectData>(MetadataRedirectData.class);

        SubnetTable subnet = mapper.add(SUBNET);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);

        return create()
                .select(mapper.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP)
                    .on(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(NETWORK_SERVICE)
                    .on(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID.eq(NETWORK_SERVICE_PROVIDER_INSTANCE_MAP.NETWORK_SERVICE_PROVIDER_ID))
                .join(NIC)
                    .on(NIC.INSTANCE_ID.eq(INSTANCE.ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .join(ipAddress)
                    .on(ipAddress.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                .join(subnet)
                    .on(subnet.ID.eq(ipAddress.SUBNET_ID))
                .where(HOST.AGENT_ID.eq(agent.getId())
                        .and(ipAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(NETWORK_SERVICE.KIND.eq(NetworkServiceConstants.KIND_METADATA))
                        .and(HOST.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                        .and(NIC.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING))
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(subnet.REMOVED.isNull()))
                .fetch().map(mapper);
    }

    @Override
    public List<? extends NetworkService> getMetadataServices(Instance instance) {
        return create()
                .select(NETWORK_SERVICE.fields())
                .from(NIC)
                .join(NETWORK_SERVICE)
                    .on(NETWORK_SERVICE.NETWORK_ID.eq(NIC.NETWORK_ID))
                .where(NETWORK_SERVICE.KIND.eq(NetworkServiceConstants.KIND_METADATA)
                        .and(NIC.INSTANCE_ID.eq(instance.getId()))
                        .and(NETWORK_SERVICE.REMOVED.isNull()))
                .fetchInto(NetworkServiceRecord.class);
    }

    @Override
    public Offering getInstanceOffering(Instance instance) {
        //TODO Add caching
        return objectManager.loadResource(Offering.class, instance.getOfferingId());
    }

    @Override
    public Zone getZone(Instance instance) {
        //TODO Add caching
        return objectManager.loadResource(Zone.class, instance.getZoneId());
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public NetworkDao getNetworkDao() {
        return networkDao;
    }

    @Inject
    public void setNetworkDao(NetworkDao networkDao) {
        this.networkDao = networkDao;
    }

}