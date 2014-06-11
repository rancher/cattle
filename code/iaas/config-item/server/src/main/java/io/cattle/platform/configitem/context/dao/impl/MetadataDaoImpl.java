package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.CredentialInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.IpAssocationTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import static io.cattle.platform.core.model.tables.VnetTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import io.cattle.platform.configitem.context.dao.MetadataDao;
import io.cattle.platform.configitem.context.data.MetadataEntry;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
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
import io.cattle.platform.core.model.tables.records.VnetRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.AggregateMultiRecordMapper;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jooq.Record;

public class MetadataDaoImpl extends AbstractJooqDao implements MetadataDao {

    NetworkDao networkDao;
    ObjectManager objectManager;

    @Override
    public List<MetadataEntry> getMetaData(Instance agentInstance) {
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

        MultiRecordMapper<MetadataEntry> mapper = new AggregateMultiRecordMapper<MetadataEntry>(MetadataEntry.class);

        InstanceTable instance = mapper.add(INSTANCE);
        NicTable nic = mapper.add(NIC);
        IpAddressTable primaryIp = mapper.add(IP_ADDRESS);
        IpAddressTable publicIp = mapper.add(IP_ADDRESS);
        VolumeTable volume = mapper.add(VOLUME);
        CredentialTable credential = mapper.add(CREDENTIAL);
        NetworkTable network = mapper.add(NETWORK);
        SubnetTable subnet = mapper.add(SUBNET);

        return create()
            .select(mapper.fields())
            .from(NIC)
            .join(instance)
                .on(NIC.INSTANCE_ID.eq(instance.ID))
            .join(nic)
                .on(instance.ID.eq(nic.ID))
            .join(network)
                .on(network.ID.eq(nic.ID))
            .join(volume)
                .on(volume.INSTANCE_ID.eq(instance.ID))
            .leftOuterJoin(CREDENTIAL_INSTANCE_MAP)
                .on(CREDENTIAL_INSTANCE_MAP.INSTANCE_ID.eq(instance.ID))
            .leftOuterJoin(credential)
                .on(CREDENTIAL_INSTANCE_MAP.CREDENTIAL_ID.eq(credential.ID))
            .leftOuterJoin(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.ID))
            .leftOuterJoin(primaryIp)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(primaryIp.ID))
            .leftOuterJoin(subnet)
                .on(primaryIp.SUBNET_ID.eq(subnet.ID))
            .leftOuterJoin(IP_ASSOCATION)
                .on(IP_ASSOCATION.CHILD_IP_ADDRESS_ID.eq(primaryIp.ID))
            .leftOuterJoin(publicIp)
                .on(IP_ASSOCATION.IP_ADDRESS_ID.eq(publicIp.ID))
            .where(NIC.VNET_ID.eq(vnet.getId())
                    .and(primaryIp.ROLE.in(IpAddressConstants.ROLE_PRIMARY, IpAddressConstants.ROLE_SECONDARY))
                    .and(publicIp.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                    .and(nic.REMOVED.isNull())
                    .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                    .and(IP_ASSOCATION.REMOVED.isNull())
                    .and(CREDENTIAL_INSTANCE_MAP.REMOVED.isNull())
                    .and(credential.REMOVED.isNull())
                    .and(credential.KIND.eq(CredentialConstants.KIND_SSH_KEY))
                    .and(publicIp.REMOVED.isNull())
                    .and(volume.REMOVED.isNull())
                    .and(primaryIp.REMOVED.isNull()))
            .fetch().map(mapper);
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