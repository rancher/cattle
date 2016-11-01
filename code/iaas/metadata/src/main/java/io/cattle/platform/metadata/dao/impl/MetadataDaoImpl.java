package io.cattle.platform.metadata.dao.impl;

import static io.cattle.platform.core.model.tables.CredentialInstanceMapTable.*;
import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Zone;
import io.cattle.platform.core.model.tables.CredentialTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NetworkTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.SubnetTable;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.AggregateMultiRecordMapper;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.metadata.dao.MetadataDao;
import io.cattle.platform.metadata.data.MetadataEntry;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

import org.jooq.ResultQuery;

public class MetadataDaoImpl extends AbstractJooqDao implements MetadataDao {

    NetworkDao networkDao;
    ObjectManager objectManager;

    private List<MetadataEntry> getMetaDataInternal(Instance agentInstance) {
        MultiRecordMapper<MetadataEntry> mapper = new AggregateMultiRecordMapper<MetadataEntry>(MetadataEntry.class);

        InstanceTable instance = mapper.add(INSTANCE);
        NicTable nic = mapper.add(NIC);
        IpAddressTable primaryIp = mapper.add(IP_ADDRESS);
        VolumeTable volume = mapper.add(VOLUME);
        CredentialTable credential = mapper.add(CREDENTIAL);
        NetworkTable network = mapper.add(NETWORK);
        SubnetTable subnet = mapper.add(SUBNET);

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
            .where(nic.INSTANCE_ID.eq(agentInstance.getId()).and(nic.REMOVED.isNull()));

        return q.fetch().map(mapper);
    }

    @Override
    public MetadataEntry getMetadataForInstance(Instance agentInstance) {
        List<MetadataEntry> entries = getMetaDataInternal(agentInstance);
        return entries.size() > 0 ? entries.get(0) : null;
    }

    @Override
    public Zone getZone(Instance instance) {
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