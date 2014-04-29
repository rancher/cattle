package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

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
    public IpAddress mapNewIpAddress(Nic nic, Object key, Object... values) {
        if ( nic.getNetworkId() == null ) {
            throw new IllegalStateException("Can not map new IP to nic with no network assigned to nic");
        }

        Map<Object,Object> inputProperties = key == null ? Collections.emptyMap() : CollectionUtils.asMap(key, values);
        Map<Object,Object> properties = CollectionUtils.asMap((Object)IP_ADDRESS.ACCOUNT_ID, nic.getAccountId(),
                IP_ADDRESS.SUBNET_ID, nic.getSubnetId(),
                IP_ADDRESS.NETWORK_ID, nic.getNetworkId());

        properties.putAll(inputProperties);
        IpAddress ipAddress = objectManager.create(IpAddress.class, objectManager.convertToPropertiesFor(IpAddress.class, properties));

        objectManager.create(IpAddressNicMap.class,
                IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID, ipAddress.getId(),
                IP_ADDRESS_NIC_MAP.NIC_ID, nic.getId());

        return ipAddress;
    }

    @Override
    public IpAddress assignNewAddress(Host host, String ipAddress) {
        IpAddress ipAddressObj = objectManager.create(IpAddress.class,
                IP_ADDRESS.ADDRESS, ipAddress,
                IP_ADDRESS.ACCOUNT_ID, host.getAccountId());

        objectManager.create(HostIpAddressMap.class,
                HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID, ipAddressObj.getId(),
                HOST_IP_ADDRESS_MAP.HOST_ID, host.getId());

        return ipAddressObj;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }


}
