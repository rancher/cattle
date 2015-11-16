package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.HOST_IP_ADDRESS_MAP;
import static io.cattle.platform.core.model.tables.HostTable.HOST;
import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class HostDaoImpl extends AbstractJooqDao implements HostDao {
    
    @Inject
    ObjectManager objectManager;

    @Override
    public List<? extends Host> getHosts(Long accountId, List<String> uuids) {
        return create()
            .selectFrom(HOST)
            .where(HOST.ACCOUNT_ID.eq(accountId)
            .and(HOST.STATE.notIn(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING))
            .and(HOST.UUID.in(uuids))).fetch();
    }

    @Override
    public List<? extends Host> getActiveHosts(Long accountId) {
        return create()
            .selectFrom(HOST)
            .where(HOST.ACCOUNT_ID.eq(accountId)
            .and(HOST.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE)))
            .fetch();
    }

    @Override
    public List<? extends IpAddress> getHostIpAddresses(long hostId) {
        return create()
                .select(IP_ADDRESS.fields())
                .from(IP_ADDRESS)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID))
                .where(HOST_IP_ADDRESS_MAP.HOST_ID.eq(hostId)
                .and(IP_ADDRESS.REMOVED.isNull())
                .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull()))
                .fetchInto(IpAddressRecord.class);
    }

    @Override
    public Host getHostForIpAddress(long ipAddressId) {
        List<? extends Host> hosts = create()
                .select(HOST.fields())
                .from(HOST)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.HOST_ID.eq(HOST.ID))
                .where(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(ipAddressId)
                        .and(HOST.REMOVED.isNull())
                        .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull()))
                .fetchInto(HostRecord.class);

        if (hosts.isEmpty()) {
            return null;
        }
        return hosts.get(0);

    }
}
