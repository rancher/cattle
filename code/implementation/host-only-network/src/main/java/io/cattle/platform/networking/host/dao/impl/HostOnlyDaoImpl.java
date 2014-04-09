package io.cattle.platform.networking.host.dao.impl;

import static io.cattle.platform.core.model.tables.HostVnetMapTable.*;
import static io.cattle.platform.core.model.tables.SubnetVnetMapTable.*;
import static io.cattle.platform.core.model.tables.VnetTable.*;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostVnetMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.SubnetVnetMap;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.tables.records.VnetRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.networking.host.contants.HostOnlyConstants;
import io.cattle.platform.networking.host.dao.HostOnlyDao;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

import org.jooq.Record;

public class HostOnlyDaoImpl extends AbstractJooqDao implements HostOnlyDao {

    ObjectManager objectManager;

    @Override
    public Vnet getVnetForHost(Network network, Host host) {
        Record record = create()
                .select(VNET.fields())
                    .from(VNET)
                    .join(HOST_VNET_MAP)
                        .on(HOST_VNET_MAP.VNET_ID.eq(VNET.ID))
                .where(VNET.NETWORK_ID.eq(network.getId())
                        .and(HOST_VNET_MAP.HOST_ID.eq(host.getId()))
                        .and(HOST_VNET_MAP.REMOVED.isNull()))
                .fetchAny();
        return record == null ? null : record.into(VnetRecord.class);
    }

    @Override
    public Vnet createVnetForHost(Network network, Host host, Subnet subnet, String uri) {
        if ( uri == null ) {
            uri = HostOnlyConstants.DEFAULT_HOST_SUBNET_URI;
        }

        Vnet vnet = objectManager.create(Vnet.class,
                VNET.URI, uri,
                VNET.ACCOUNT_ID, network.getAccountId(),
                VNET.NETWORK_ID, network.getId());

        objectManager.create(HostVnetMap.class,
                HOST_VNET_MAP.VNET_ID, vnet.getId(),
                HOST_VNET_MAP.HOST_ID, host.getId());

        if ( subnet != null ) {
            objectManager.create(SubnetVnetMap.class,
                    SUBNET_VNET_MAP.VNET_ID, vnet.getId(),
                    SUBNET_VNET_MAP.SUBNET_ID, subnet.getId());
        }

        return vnet;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
