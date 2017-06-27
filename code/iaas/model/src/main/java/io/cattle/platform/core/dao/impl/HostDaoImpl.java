package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.PhysicalHostRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.util.resource.UUID;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.RecordHandler;


public class HostDaoImpl extends AbstractJooqDao implements HostDao {

    ObjectManager objectManager;
    GenericResourceDao genericResourceDao;
    TransactionDelegate transaction;
    Long startTime;


    public HostDaoImpl(Configuration configuration, ObjectManager objectManager, GenericResourceDao genericResourceDao, TransactionDelegate transaction) {
        super(configuration);
        this.objectManager = objectManager;
        this.genericResourceDao = genericResourceDao;
        this.transaction = transaction;
    }

    @Override
    public boolean hasActiveHosts(Long accountId) {
        List<?> result = create()
            .select(HOST.ID)
            .from(HOST)
            .join(AGENT)
                .on(AGENT.ID.eq(HOST.AGENT_ID))
            .where(HOST.ACCOUNT_ID.eq(accountId)
                        .and(HOST.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                                CommonStatesConstants.UPDATING_ACTIVE)
                                .and(AGENT.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE,
                                        AgentConstants.STATE_FINISHING_RECONNECT, AgentConstants.STATE_RECONNECTED))))
            .limit(1)
            .fetch();
        return result.size() > 0;
    }

    @Override
    public Map<Long, List<Object>> getInstancesPerHost(List<Long> hosts, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();
        create().select(INSTANCE.ID, INSTANCE.HOST_ID)
            .from(INSTANCE)
            .where(INSTANCE.HOST_ID.in(hosts)
                    .and(INSTANCE.REMOVED.isNull()))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long hostId = record.getValue(INSTANCE.HOST_ID);
                    Long instanceId = record.getValue(INSTANCE.ID);
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
    public PhysicalHost createMachineForHost(final Host host, String driver) {
        return transaction.doInTransactionResult(() -> {
            String uuid = UUID.randomUUID().toString();
            final Map<Object, Object> data = new HashMap<>(DataUtils.getFields(host));
            data.put(PHYSICAL_HOST.KIND, MachineConstants.KIND_MACHINE);
            data.put(PHYSICAL_HOST.NAME, DataAccessor.fieldString(host, HostConstants.FIELD_HOSTNAME));
            data.put(PHYSICAL_HOST.DESCRIPTION, host.getDescription());
            data.put(PHYSICAL_HOST.ACCOUNT_ID, host.getAccountId());
            data.put(PHYSICAL_HOST.EXTERNAL_ID, uuid);
            data.put(PHYSICAL_HOST.DRIVER, driver);

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
        });
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

    @Override
    public void updateNullUpdatedHosts() {
        if (HOST_REMOVE_DELAY.get() < 0) {
            return;
        }

        create().update(HOST)
            .set(HOST.REMOVE_AFTER, new Date(System.currentTimeMillis() + HOST_REMOVE_DELAY.get() * 1000))
            .where(HOST.REMOVE_AFTER.isNull())
            .execute();
    }

    @Override
    public List<? extends Host> findHostsRemove() {
        if (startTime == null) {
            startTime = System.currentTimeMillis();
        }

        if ((System.currentTimeMillis() - startTime) <= (HOST_REMOVE_START_DELAY.get() * 1000)) {
            return Collections.emptyList();
        }

        return create().select(HOST.fields())
                .from(HOST)
                .join(AGENT)
                    .on(AGENT.ID.eq(HOST.AGENT_ID))
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(HOST.ACCOUNT_ID))
                .where(AGENT.STATE.eq(AgentConstants.STATE_DISCONNECTED)
                        .and(HOST.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.INACTIVE))
                        .and(HOST.REMOVE_AFTER.lt(new Date())))
                .fetchInto(HostRecord.class);
    }
}
