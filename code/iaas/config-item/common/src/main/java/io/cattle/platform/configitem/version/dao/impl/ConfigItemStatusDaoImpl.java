package io.cattle.platform.configitem.version.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.ConfigItemStatusTable.*;
import static io.cattle.platform.core.model.tables.ConfigItemTable.*;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.model.Client;
import io.cattle.platform.configitem.model.DefaultItemVersion;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.dao.ConfigItemStatusDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ConfigItem;
import io.cattle.platform.core.model.ConfigItemStatus;
import io.cattle.platform.core.model.tables.records.ConfigItemStatusRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
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
import org.jooq.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

public class ConfigItemStatusDaoImpl extends AbstractJooqDao implements ConfigItemStatusDao {

    private static final Logger log = LoggerFactory.getLogger(ConfigItemStatusDaoImpl.class);
    private static final DynamicIntProperty BATCH_SIZE = ArchaiusUtil.getInt("item.sync.batch.size");

    ObjectManager objectManager;

    @Override
    public long incrementOrApply(Client client, String itemName) {
        if ( ! increment(client, itemName) ) {
            RuntimeException e = apply(client, itemName);
            if ( e != null ) {
                if ( ! increment(client, itemName) ) {
                    throw new IllegalStateException("Failed to increment [" + itemName + "] on [" + client + "]", e);
                }
            }
        }

        return getRequestedVersion(client, itemName);
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
                        CONFIG_ITEM_STATUS.AGENT_ID,
                        CONFIG_ITEM_STATUS.REQUESTED_VERSION,
                        CONFIG_ITEM_STATUS.REQUESTED_UPDATED)
                .values(
                        itemName,
                        new Long(client.getResourceId()),
                        1L,
                        new Timestamp(System.currentTimeMillis()))
                .execute();
            return null;
        } catch ( DataAccessException e ) {
            return e;
        }
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

        return updated == 1;
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
        if ( client.getResourceType() != Agent.class ) {
            throw new IllegalArgumentException("Only Agent.class is supported for Client type");
        }

        return CONFIG_ITEM_STATUS.AGENT_ID.eq(client.getResourceId());
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
    public Map<Long, List<String>> findOutOfSync(boolean migration) {
        Map<Long,List<String>> result = new HashMap<Long, List<String>>();

        for ( ConfigItemStatus status : (migration ? migrationItems() : outOfSyncItems()) ) {
            CollectionUtils.addToMap(result, status.getAgentId(), status.getName(), ArrayList.class);
        }

        return result;
    }

    protected List<? extends ConfigItemStatus> outOfSyncItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(AGENT)
                    .on(AGENT.ID.eq(CONFIG_ITEM_STATUS.AGENT_ID))
                .where(CONFIG_ITEM_STATUS.REQUESTED_VERSION.ne(CONFIG_ITEM_STATUS.APPLIED_VERSION))
                .orderBy(AGENT.AGENT_GROUP_ID.asc(), AGENT.ID.asc())
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }

    protected List<? extends ConfigItemStatus> migrationItems() {
        return create()
                .select(CONFIG_ITEM_STATUS.fields())
                .from(CONFIG_ITEM_STATUS)
                .join(AGENT)
                    .on(AGENT.ID.eq(CONFIG_ITEM_STATUS.AGENT_ID))
                .join(CONFIG_ITEM)
                    .on(CONFIG_ITEM.NAME.eq(CONFIG_ITEM_STATUS.NAME))
                .where(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNotNull()
                        .and(CONFIG_ITEM_STATUS.SOURCE_VERSION.ne(CONFIG_ITEM.SOURCE_VERSION)))
                .orderBy(AGENT.AGENT_GROUP_ID.asc(), AGENT.ID.asc())
                .limit(BATCH_SIZE.get())
                .fetchInto(ConfigItemStatusRecord.class);
    }
}
