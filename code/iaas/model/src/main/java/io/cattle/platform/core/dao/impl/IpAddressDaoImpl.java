package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.IpAssociationTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.IpPoolConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.IpAssociation;
import io.cattle.platform.core.model.IpPool;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class IpAddressDaoImpl extends AbstractJooqDao implements IpAddressDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager processManager;

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
    public IpAddress getPrimaryAssociatedIpAddress(IpAddress ipAddress) {
        if ( ipAddress == null ) {
            return null;
        }

        List<? extends IpAddress> ips = create().select(IP_ADDRESS.fields())
                .from(IP_ASSOCIATION)
                .join(IP_ADDRESS)
                .on(IP_ADDRESS.ID.eq(IP_ASSOCIATION.IP_ADDRESS_ID))
                .where(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PUBLIC)
                        .and(IP_ASSOCIATION.CHILD_IP_ADDRESS_ID.eq(ipAddress.getId()))
                        .and(IP_ASSOCIATION.REMOVED.isNull()))
                        .fetchInto(IpAddressRecord.class);

        return ips.size() == 0 ? null : ips.get(0);
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

    @Override
    public IpAddress assignAndActivateNewAddress(Host host, String ipAddress) {
        IpAddress ipAddressObj = objectManager.create(IpAddress.class,
                IP_ADDRESS.ADDRESS, ipAddress,
                IP_ADDRESS.ACCOUNT_ID, host.getAccountId());

        HostIpAddressMap map = objectManager.create(HostIpAddressMap.class,
                HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID, ipAddressObj.getId(),
                HOST_IP_ADDRESS_MAP.HOST_ID, host.getId());

        processManager.scheduleStandardProcess(StandardProcess.CREATE, ipAddressObj, null);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, map, null);

        return ipAddressObj;
    }

    @Override
    public IpAddress updateIpAddress(IpAddress ipAddress, String newIpAddress) {
        objectManager.setFields(ipAddress, IP_ADDRESS.ADDRESS, newIpAddress);
        processManager.scheduleStandardProcess(StandardProcess.UPDATE, ipAddress, null);
        return ipAddress;
    }

    @Override
    public IpAddress createIpAddressFromPool(IpPool pool, Object key, Object... value) {
        Map<Object,Object> data = new HashMap<Object, Object>();

        data.put(IP_ADDRESS.KIND, IpAddressConstants.KIND_POOLED_IP_ADDRESS);
        data.put(IP_ADDRESS.IP_POOL_ID, pool.getId());
        data.put(IP_ADDRESS.ROLE, IpAddressConstants.ROLE_PUBLIC);
        data.put(ObjectMetaDataManager.CAPABILITIES_FIELD, Arrays.asList(IpAddressConstants.CAPABILITY_ASSOCIATE));

        if ( IpPoolConstants.KIND_SUBNET_IP_POOL.equals(pool.getKind()) ) {
            List<Subnet> subnets = objectManager.children(pool, Subnet.class);

            for ( Subnet subnet : subnets ) {
                if ( CommonStatesConstants.ACTIVE.equals(subnet.getState()) ) {
                    data.put(IP_ADDRESS.SUBNET_ID, subnet.getId());
                    break;
                }
            }
        }

        if ( key != null ) {
            Map<Object,Object> override = CollectionUtils.asMap(key, value);
            data.putAll(override);
        }

        Map<String,Object> props = objectManager.convertToPropertiesFor(IpAddress.class, data);
        return objectManager.create(IpAddress.class, props);
    }

    @Override
    public IpAssociation createOrFindAssociation(IpAddress address, IpAddress childIpAddress) {
        IpAssociation association = objectManager.findOne(IpAssociation.class,
                IP_ASSOCIATION.IP_ADDRESS_ID, address.getId(),
                IP_ASSOCIATION.CHILD_IP_ADDRESS_ID, childIpAddress.getId(),
                IP_ASSOCIATION.REMOVED, null);

        if ( association == null ) {
            association = objectManager.create(IpAssociation.class,
                    IP_ASSOCIATION.IP_ADDRESS_ID, address.getId(),
                    IP_ASSOCIATION.CHILD_IP_ADDRESS_ID, childIpAddress.getId(),
                    IP_ASSOCIATION.ACCOUNT_ID, childIpAddress.getAccountId());
        }

        return association;
    }

    @Override
    public IpAddress getInstancePrimaryIp(Instance instance) {
        IpAddress ip = null;
        for (Nic nic : objectManager.children(instance, Nic.class)) {
            ip = getPrimaryIpAddress(nic);
            if (ip != null) {
                break;
            }
        }
        return ip;
    }
}
