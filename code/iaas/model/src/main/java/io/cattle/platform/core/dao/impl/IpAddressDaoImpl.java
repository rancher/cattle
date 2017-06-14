package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.dao.IpAddressDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpAddressDaoImpl extends AbstractJooqDao implements IpAddressDao {

    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    TransactionDelegate transaction;

    @Override
    public IpAddress getPrimaryIpAddress(Nic nic) {
        List<? extends IpAddress> ipAddresses = create()
                .select(IP_ADDRESS.fields())
                .from(IP_ADDRESS)
                .join(IP_ADDRESS_NIC_MAP)
                .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID)
                        .and(IP_ADDRESS_NIC_MAP.NIC_ID.eq(nic.getId())))
                        .where(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                                .fetchInto(IpAddressRecord.class);

        if (ipAddresses.size() == 1) {
            return ipAddresses.get(0);
        }

        for (IpAddress ipAddress : ipAddresses) {
            if (IpAddressConstants.ROLE_PRIMARY.equals(ipAddress.getRole())) {
                return ipAddress;
            }
        }

        return null;
    }

    @Override
    public IpAddress mapNewIpAddress(Nic nic, Object key, Object... values) {
        return transaction.doInTransactionResult(() -> {
            if ( nic.getNetworkId() == null ) {
                throw new IllegalStateException("Can not map new IP to nic with no network assigned to nic");
            }

            Map<Object,Object> inputProperties = key == null ? Collections.emptyMap() : CollectionUtils.asMap(key, values);
            Map<Object,Object> properties = CollectionUtils.asMap((Object)IP_ADDRESS.ACCOUNT_ID, nic.getAccountId());

            properties.putAll(inputProperties);
            IpAddress ipAddress = objectManager.create(IpAddress.class, objectManager.convertToPropertiesFor(IpAddress.class, properties));

            objectManager.create(IpAddressNicMap.class,
                    IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID, ipAddress.getId(),
                    IP_ADDRESS_NIC_MAP.NIC_ID, nic.getId());

            return ipAddress;
        });
    }

    @Override
    public IpAddress assignAndActivateNewAddress(Host host, String ipAddress) {
        return transaction.doInTransactionResult(() -> {
            IpAddress ipAddressObj = objectManager.create(IpAddress.class,
                    IP_ADDRESS.ADDRESS, ipAddress,
                    IP_ADDRESS.ACCOUNT_ID, host.getAccountId());

            HostIpAddressMap map = objectManager.create(HostIpAddressMap.class,
                    HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID, ipAddressObj.getId(),
                    HOST_IP_ADDRESS_MAP.HOST_ID, host.getId());

            processManager.scheduleStandardProcess(StandardProcess.CREATE, ipAddressObj, null);
            processManager.scheduleStandardProcess(StandardProcess.CREATE, map, null);

            return ipAddressObj;
        });
    }

    @Override
    public IpAddress updateIpAddress(IpAddress ipAddress, String newIpAddress) {
        return transaction.doInTransactionResult(() -> {
            Map<String, Object> data = new HashMap<>();
            if ( ipAddress.getAddress() != null ) {
                Map<String, Object> old = new HashMap<>();
                old.put(IpAddressConstants.FIELD_ADDRESS, ipAddress.getAddress());
                data.put("old", old);
            }
            objectManager.setFields(ipAddress, IP_ADDRESS.ADDRESS, newIpAddress);
            processManager.scheduleStandardProcess(StandardProcess.UPDATE, ipAddress, data);
            return ipAddress;
        });
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

    @Override
    public List<? extends IpAddress> findBadHostIpAddress(int count) {
        return create().select(IP_ADDRESS.fields())
                .from(IP_ADDRESS)
                .join(HOST_IP_ADDRESS_MAP)
                    .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID))
                .join(HOST)
                    .on(HOST.ID.eq(HOST_IP_ADDRESS_MAP.HOST_ID))
                .where(HOST.STATE.eq(AccountConstants.STATE_PURGED)
                        .and(IP_ADDRESS.REMOVED.isNull())
                        .and(IP_ADDRESS.STATE.notIn(CommonStatesConstants.REMOVING)))
                .limit(count)
                .fetchInto(IpAddressRecord.class);
    }
}
