package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.core.model.tables.records.PhysicalHostRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.util.resource.UUID;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.jooq.Record2;
import org.jooq.RecordHandler;


public class HostDaoImpl extends AbstractJooqDao implements HostDao {

    @Inject
    ObjectManager objectManager;
    @Inject
    GenericResourceDao genericResourceDao;

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
    public IpAddress getIpAddressForHost(Long hostId) {
        List<? extends IpAddress> result = create()
            .select(IP_ADDRESS.fields())
                .from(IP_ADDRESS)
                .join(HOST_IP_ADDRESS_MAP)
                    .on(IP_ADDRESS.ID.eq(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID))
                .where(HOST_IP_ADDRESS_MAP.HOST_ID.eq(hostId)
                    .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull())
                    .and(IP_ADDRESS.REMOVED.isNull()))
            .fetchInto(IpAddressRecord.class);
        return result.size() == 0 ? null : result.get(0);
    }

    @Override
    public Host getHostForIpAddress(long ipAddressId) {
        List<? extends Host> hosts = create()
                .select(HOST.fields())
                .from(HOST)
                .join(HOST_IP_ADDRESS_MAP)
                .on(HOST_IP_ADDRESS_MAP.HOST_ID.eq(HOST.ID))
                .where(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(ipAddressId)
                        .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull()))
                .fetchInto(HostRecord.class);

        if (hosts.isEmpty()) {
            return null;
        }
        return hosts.get(0);

    }

    @Override
    public Map<Long, List<Object>> getInstancesPerHost(List<Long> hosts, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();
        create().select(INSTANCE_HOST_MAP.INSTANCE_ID, INSTANCE_HOST_MAP.HOST_ID)
            .from(INSTANCE_HOST_MAP)
            .join(INSTANCE)
                .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
            .where(INSTANCE_HOST_MAP.REMOVED.isNull()
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE_HOST_MAP.HOST_ID.in(hosts)))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long hostId = record.getValue(INSTANCE_HOST_MAP.HOST_ID);
                    Long instanceId = record.getValue(INSTANCE_HOST_MAP.INSTANCE_ID);
                    List<Object> list = result.get(hostId);
                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(hostId, list);
                    }
                    list.add(idFormatter.formatId(InstanceConstants.TYPE, instanceId));
                }
            });

        return result;
    }

    @Override
    public PhysicalHost createMachineForHost(final Host host) {
        String uuid = UUID.randomUUID().toString();
        final Map<Object, Object> data = new HashMap<Object, Object>(DataUtils.getFields(host));
        data.put(PHYSICAL_HOST.KIND, MachineConstants.KIND_MACHINE);
        data.put(PHYSICAL_HOST.NAME, DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME));
        data.put(PHYSICAL_HOST.DESCRIPTION, host.getDescription());
        data.put(PHYSICAL_HOST.ACCOUNT_ID, host.getAccountId());
        data.put(PHYSICAL_HOST.EXTERNAL_ID, uuid);

        PhysicalHost phyHost = DeferredUtils.nest(new Callable<PhysicalHost>() {
            @Override
            public PhysicalHost call() throws Exception {
                return genericResourceDao.createAndSchedule(PhysicalHost.class, objectManager.convertToPropertiesFor(PhysicalHost.class, data));
            }
        });

        objectManager.setFields(host,
                HOST.PHYSICAL_HOST_ID, phyHost.getId(),
                HostConstants.FIELD_REPORTED_UUID, uuid);

        return phyHost;
    }

    @Override
    public Map<Long, PhysicalHost> getPhysicalHostsForHosts(List<Long> hostIds) {
        Map<Long, PhysicalHost> hosts = new HashMap<>();
        List<? extends PhysicalHost> hostList = create().select(PHYSICAL_HOST.fields())
            .from(PHYSICAL_HOST)
            .join(HOST)
                .on(HOST.PHYSICAL_HOST_ID.eq(PHYSICAL_HOST.ID))
            .where(HOST.ID.in(hostIds))
            .fetchInto(PhysicalHostRecord.class);
        for (PhysicalHost host : hostList) {
            hosts.put(host.getId(), host);
        }
        return hosts;
    }
}
