package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;

import java.util.List;

import javax.inject.Inject;

import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

public class IpAddressDaoImpl extends AbstractJooqDao implements IpAddressDao {

    ObjectManager objectManager;

    @Override
    public IpAddress getPrimaryIpAddress(Nic nic) {
        Long subnetId = nic.getSubnetId();

        if ( subnetId == null ) {
            return null;
        }

        List<? extends IpAddress> ipAddresses = create()
                .select(IP_ADDRESS.fields())
                    .from(IP_ADDRESS)
                    .join(IP_ADDRESS_NIC_MAP)
                        .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID)
                                .and(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.getId())))
                    .where(IP_ADDRESS.SUBNET_ID.eq(subnetId)
                            .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull()))
                    .fetchInto(IpAddressRecord.class);

        return ipAddresses.size() > 0 ? ipAddresses.get(0) : null;
    }

    @Override
    public IpAddress mapNewIpAddress(Nic nic) {
        if ( nic.getSubnetId() == null ) {
            throw new IllegalStateException("Can not map new IP to nic with no subnet assigned to nic");
        }

        if ( nic.getNetworkId() == null ) {
            throw new IllegalStateException("Can not map new IP to nic with no network assigned to nic");
        }

        IpAddress ipAddress = objectManager.create(IpAddress.class,
                IP_ADDRESS.ACCOUNT_ID, nic.getAccountId(),
                IP_ADDRESS.SUBNET_ID, nic.getSubnetId(),
                IP_ADDRESS.NETWORK_ID, nic.getNetworkId());

        objectManager.create(IpAddressNicMap.class,
                IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID, ipAddress.getId(),
                IP_ADDRESS_NIC_MAP.NIC_ID, nic.getId());

        return ipAddress;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
