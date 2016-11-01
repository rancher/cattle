package io.cattle.platform.configitem.version.dao.impl;

import static io.cattle.platform.core.model.tables.AccountTable.*;
import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.ConfigItemStatusTable.*;
import static io.cattle.platform.core.model.tables.ConfigItemTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.events.ConfigUpdated;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.DefaultItemVersion;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.dao.ConfigItemStatusDao;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ConfigItem;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.records.ConfigItemStatusRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Condition;
import org.jooq.Record2;
import org.jooq.TableField;
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.netflix.config.DynamicIntProperty;

public class ConfigItemStatusDaoImpl extends AbstractJooqDao implements ConfigItemStatusDao {


    private static final Logger log = LoggerFactory.getLogger(ConfigItemStatusDaoImpl.class);
    private static final DynamicIntProperty BATCH_SIZE = ArchaiusUtil.getInt("item.sync.batch.size");

    @Inject
    EventService eventService;
    ObjectManager objectManager;
    Timer incrementTimer = MetricsUtil.getRegistry().timer("config.item.increment");
    Timer appliedTimer = MetricsUtil.getRegistry().timer("config.item.applied");

    @Override
    public void incrementOrApply(Client client, String itemName) {
        Context t = incrementTimer.time();
        try {
            if ( ! increment(client, itemName) ) {
                RuntimeException e = apply(client, itemName);
                if ( e != null ) {
                    if ( ! increment(client, itemName) ) {
                        throw new IllegalStateException("Failed to increment [" + itemName + "] on [" + client + "]", e);
                    }
                }
            }
        } finally {
            t.stop();
        }
    }


    @Override
    public Long getRequestedVersion(Client client, String itemName) {
        return create()
                .select(CONFIG_ITEM_STATUS.REQUESTED_VERSION)
                .from(CONFIG_ITEM_STATUS)
                .where(
                        CONFIG_ITEM_STATUS.NAME.eq(itemName)
                        .and(targetObjectCondition(client)))
                .fetchOneInto(Long.class);
    }

    protected boolean increment(Client client, String itemName) {
        int updated = create()
            .update(CONFIG_ITEM_STATUS)
                .set(CONFIG_ITEM_STATUS.REQUESTED_VERSION, CONFIG_ITEM_STATUS.REQUESTED_VERSION.plus(1))
                .set(CONFIG_ITEM_STATUS.REQUESTED_UPDATED, new Timestamp(System.currentTimeMillis()))
            .where(
                    CONFIG_ITEM_STATUS.NAME.eq(itemName)
                    .and(targetObjectCondition(client))).execute();

        return updated > 0;
    }

    protected RuntimeException apply(Client client, String itemName) {
        try {
            create()
                .insertInto(CONFIG_ITEM_STATUS,
                        CONFIG_ITEM_STATUS.NAME,
                        getResourceField(client),
                        CONFIG_ITEM_STATUS.RESOURCE_TYPE,
                        CONFIG_ITEM_STATUS.RESOURCE_ID,
                        CONFIG_ITEM_STATUS.REQUESTED_VERSION,
                        CONFIG_ITEM_STATUS.REQUESTED_UPDATED)
                .values(
                        itemName,
                        new Long(client.getResourceId()),
                        getResourceNameField(client),
                        new Long(client.getResourceId()),
                        1L,
                        new Timestamp(System.currentTimeMillis()))
                .execute();
            return null;
        } catch ( DataAccessException e ) {
            return e;
        }
    }

    protected String getResourceNameField(Client client) {
        return getResourceField(client).getName().toLowerCase();
    }

    protected TableField<ConfigItemStatusRecord, Long> getResourceField(Client client) {
        if ( client.getResourceType() == Agent.class ) {
            return CONFIG_ITEM_STATUS.AGENT_ID;
        }
        if ( client.getResourceType() == Service.class ) {
            return CONFIG_ITEM_STATUS.SERVICE_ID;
        }

        if (client.getResourceType() == Stack.class) {
            return CONFIG_ITEM_STATUS.STACK_ID;
        }

        if ( client.getResourceType() == Account.class ) {
            return CONFIG_ITEM_STATUS.ACCOUNT_ID;
        }

        if (client.getResourceType() == Host.class) {
            return CONFIG_ITEM_STATUS.HOST_ID;
        }

        throw new IllegalArgumentException("Unsupported client type [" + client.getResourceType() + "]");
    }

    @Override
    public boolean isAssigned(Client client, String itemName) {
        ConfigItemStatus status = create()
                .selectFrom(CONFIG_ITEM_STATUS)
                .where(
                        CONFIG_ITEM_STATUS.NAME.eq(itemName)
                        .and(targetObjectCondition(client)))
                .fetchAny();

        return status != null;
    }

    @Override
    public boolean setApplied(Client client, String itemName, ItemVersion version) {
        Context t = appliedTimer.time();
        try {
            int updated = update(CONFIG_ITEM_STATUS)
                .set(CONFIG_ITEM_STATUS.APPLIED_VERSION, version.getRevision())
                .set(CONFIG_ITEM_STATUS.SOURCE_VERSION, version.getSourceRevision())
                .set(CONFIG_ITEM_STATUS.APPLIED_UPDATED, new Timestamp(System.currentTimeMillis()))
                .where(
                        CONFIG_ITEM_STATUS.NAME.eq(itemName)
                        .and(targetObjectCondition(client)))
                .execute();

            if ( updated > 1 ) {
                log.error("Updated too many rows [{}] for client [{}] itemName [{}] itemVersion [{}]",
                        updated, client, itemName, version);
            }

            if (updated == 1) {
                ConfigUpdated event = new ConfigUpdated(client.getResourceType(), client.getResourceId(), itemName);
                event.withResourceType(objectManager.getType(client.getResourceType())).withResourceId(Long.toString(client.getResourceId()));
                DeferredUtils.deferPublish(eventService, event);
            }

            return false;
        } finally {
            t.stop();
        }
    }


    @Override
    public void setLatest(Client client, String itemName, String sourceRevision) {
        update(CONFIG_ITEM_STATUS)
                .set(CONFIG_ITEM_STATUS.APPLIED_VERSION, CONFIG_ITEM_STATUS.REQUESTED_VERSION)
                .set(CONFIG_ITEM_STATUS.SOURCE_VERSION, sourceRevision)
                .where(
                        CONFIG_ITEM_STATUS.NAME.eq(itemName)
                        .and(targetObjectCondition(client))
                )
                .execute();
    }

    protected Condition targetObjectCondition(Client client) {
        return CONFIG_ITEM_STATUS.RESOURCE_TYPE.eq(getResourceNameField(client))
                .and(CONFIG_ITEM_STATUS.RESOURCE_ID.eq(client.getResourceId()));
    }

    @Override
    public void setItemSourceVersion(String name, String sourceRevision) {
        ConfigItem item = create()
                    .selectFrom(CONFIG_ITEM)
                    .where(
                            CONFIG_ITEM.NAME.eq(name))
                    .fetchOne();
        if ( item != null && sourceRevision.equals(item.getSourceVersion()) ) {
            return;
        }

        log.info("Setting config [{}] to source version [{}]", name, sourceRevision);
        int updated = create()
                .update(CONFIG_ITEM)
                    .set(CONFIG_ITEM.SOURCE_VERSION, sourceRevision)
                .where(
                        CONFIG_ITEM.NAME.eq(name))
                .execute();

        if ( updated == 0 ) {
            create()
                .insertInto(CONFIG_ITEM, CONFIG_ITEM.NAME, CONFIG_ITEM.SOURCE_VERSION)
                .values(name, sourceRevision)
                .execute();
        }
    }

    @Override
    public List<? extends ConfigItemStatus> listItems(ConfigUpdateRequest request) {
        Set<String> names = new HashSet<String>();

        for ( ConfigUpdateItem item : request.getItems() ) {
            names.add(item.getName());
        }

        return create()
                .selectFrom(CONFIG_ITEM_STATUS)
                .where(
                        CONFIG_ITEM_STATUS.NAME.in(names)
                        .and(targetObjectCondition(request.getClient())))
                .fetch();
    }

    @Override
    public ItemVersion getRequestedItemVersion(Client client, String itemName) {
        Record2<Long,String> result = create()
                .select(CONFIG_ITEM_STATUS.REQUESTED_VERSION, CONFIG_ITEM.SOURCE_VERSION)
                .from(CONFIG_ITEM_STATUS)
                .join(CONFIG_ITEM)
                    .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .where(
                        CONFIG_ITEM_STATUS.NAME.eq(itemName)
                        .and(targetObjectCondition(client)))
                .fetchOne();

        return result == null ? null : new DefaultItemVersion(result.value1(), result.value2());
    }


    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Map<Client, List<String>> findOutOfSync(boolean migration) {
        Map<Client, List<String>> result = new HashMap<>();

        for ( ConfigItemStatus status : (migration ? serviceMigrationItems() : serviceOutOfSyncItems()) ) {
            Client client = new Client(status);
            CollectionUtils.addToMap(result, client, status.getName(), ArrayList.class);
        }

        for (ConfigItemStatus status : (migration ? stackMigrationItems() : stackOutOfSyncItems())) {
            Client client = new Client(status);
            CollectionUtils.addToMap(result, client, status.getName(), ArrayList.class);
        }

        for ( ConfigItemStatus status : (migration ? agentMigrationItems() : agentOutOfSyncItems()) ) {
            Client client = new Client(status);
            CollectionUtils.addToMap(result, client, status.getName(), ArrayList.class);
        }

        for ( ConfigItemStatus status : (migration ? accountMigrationItems() : accountOutOfSyncItems()) ) {
            Client client = new Client(status);
            CollectionUtils.addToMap(result, client, status.getName(), ArrayList.class);
        }

        for (ConfigItemStatus status : (migration ? hostMigrationItems() : hostOutOfSyncItems())) {
            Client client = new Client(status);
            CollectionUtils.addToMap(result, client, status.getName(), ArrayList.class);
        }

        return result;
    }

    @Override
    public Map<String, ItemVersion> getApplied(Client client) {
        Map<String, ItemVersion> versions = new HashMap<>();
        List<ConfigItemStatusRecord> records = create()
                .selectFrom(CONFIG_ITEM_STATUS)
                .where(targetObjectCondition(client))
                .fetch();

        for (ConfigItemStatusRecord record : records) {
            Long applied = record.getAppliedVersion();
            if (applied == null) {
                continue;
            }
            versions.put(record.getName(), new DefaultItemVersion(record.getAppliedVersion(), ""));
        }

        return versions;
    }

    protected List<? extends ConfigItemStatus> serviceOutOfSyncItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(SERVICE)
                .on(SERVICE.ID.eq(CONFIG_ITEM_STATUS.SERVICE_ID))
                .where(CONFIG_ITEM_STATUS.REQUESTED_VERSION.ne(CONFIG_ITEM_STATUS.APPLIED_VERSION)
                        .and(SERVICE.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> stackOutOfSyncItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(STACK)
                .on(STACK.ID.eq(CONFIG_ITEM_STATUS.STACK_ID))
                .where(CONFIG_ITEM_STATUS.REQUESTED_VERSION.ne(CONFIG_ITEM_STATUS.APPLIED_VERSION)
                        .and(STACK.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> serviceMigrationItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(SERVICE)
                .on(SERVICE.ID.eq(CONFIG_ITEM_STATUS.SERVICE_ID))
                .join(CONFIG_ITEM)
                .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .where(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNotNull()
                        .and(CONFIG_ITEM_STATUS.SOURCE_VERSION.ne(CONFIG_ITEM.SOURCE_VERSION))
                        .and(SERVICE.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> hostMigrationItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(HOST)
                .on(HOST.ID.eq(CONFIG_ITEM_STATUS.HOST_ID))
                .join(CONFIG_ITEM)
                .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .where(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNotNull()
                        .and(CONFIG_ITEM_STATUS.SOURCE_VERSION.ne(CONFIG_ITEM.SOURCE_VERSION))
                        .and(HOST.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> stackMigrationItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(STACK)
                .on(STACK.ID.eq(CONFIG_ITEM_STATUS.STACK_ID))
                .join(CONFIG_ITEM)
                .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .where(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNotNull()
                        .and(CONFIG_ITEM_STATUS.SOURCE_VERSION.ne(CONFIG_ITEM.SOURCE_VERSION))
                        .and(STACK.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> accountOutOfSyncItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(ACCOUNT)
                .on(ACCOUNT.ID.eq(CONFIG_ITEM_STATUS.ACCOUNT_ID))
                .where(CONFIG_ITEM_STATUS.REQUESTED_VERSION.ne(CONFIG_ITEM_STATUS.APPLIED_VERSION)
                        .and(ACCOUNT.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> accountMigrationItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(ACCOUNT)
                .on(ACCOUNT.ID.eq(CONFIG_ITEM_STATUS.ACCOUNT_ID))
                .join(CONFIG_ITEM)
                .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .where(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNotNull()
                        .and(CONFIG_ITEM_STATUS.SOURCE_VERSION.ne(CONFIG_ITEM.SOURCE_VERSION))
                        .and(ACCOUNT.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> agentOutOfSyncItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(AGENT)
                .on(AGENT.ID.eq(CONFIG_ITEM_STATUS.AGENT_ID))
                .leftOuterJoin(INSTANCE)
                .on(INSTANCE.AGENT_ID.eq(AGENT.ID))
                .where(CONFIG_ITEM_STATUS.REQUESTED_VERSION.ne(CONFIG_ITEM_STATUS.APPLIED_VERSION)
                        .and(AGENT.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.ACTIVATING,
                                AgentConstants.STATE_RECONNECTING, AgentConstants.STATE_FINISHING_RECONNECT,
                                AgentConstants.STATE_RECONNECTED))
                        .and(INSTANCE.STATE.isNull().or(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING))))
                .orderBy(AGENT.ID.asc())
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> hostOutOfSyncItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(HOST)
                .on(HOST.ID.eq(CONFIG_ITEM_STATUS.HOST_ID))
                .where(CONFIG_ITEM_STATUS.REQUESTED_VERSION.ne(CONFIG_ITEM_STATUS.APPLIED_VERSION)
                        .and(HOST.REMOVED.isNull()))
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> agentMigrationItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(AGENT)
                .on(AGENT.ID.eq(CONFIG_ITEM_STATUS.AGENT_ID))
                .join(CONFIG_ITEM)
                .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .leftOuterJoin(INSTANCE)
                .on(INSTANCE.AGENT_ID.eq(AGENT.ID))
                .where(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNotNull()
                        .and(CONFIG_ITEM_STATUS.SOURCE_VERSION.ne(CONFIG_ITEM.SOURCE_VERSION))
                        .and(AGENT.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.ACTIVATING,
                                AgentConstants.STATE_RECONNECTING, AgentConstants.STATE_FINISHING_RECONNECT,
                                AgentConstants.STATE_RECONNECTED))
                        .and(INSTANCE.STATE.isNull().or(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING))))
                .orderBy(AGENT.ID.asc())
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }
}
