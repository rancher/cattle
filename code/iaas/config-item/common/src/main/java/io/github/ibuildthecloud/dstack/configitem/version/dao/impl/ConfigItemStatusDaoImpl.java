package io.github.ibuildthecloud.dstack.configitem.version.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.ConfigItemStatusTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.ConfigItemTable.*;
import io.github.ibuildthecloud.dstack.configitem.model.Client;
import io.github.ibuildthecloud.dstack.configitem.model.ItemVersion;
import io.github.ibuildthecloud.dstack.configitem.version.dao.ConfigItemStatusDao;
import io.github.ibuildthecloud.dstack.core.model.ConfigItemStatus;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.object.ObjectManager;

import javax.inject.Inject;

import org.jooq.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigItemStatusDaoImpl extends AbstractJooqDao implements ConfigItemStatusDao {

    private static final Logger log = LoggerFactory.getLogger(ConfigItemStatusDaoImpl.class);

    ObjectManager objectManager;

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
            .where(
                    CONFIG_ITEM_STATUS.SOURCE_VERSION.eq(version.getSourceRevision()).or(CONFIG_ITEM_STATUS.SOURCE_VERSION.isNull())
                    .and(CONFIG_ITEM_STATUS.NAME.eq(itemName))
                    .and(targetObjectCondition(client))
            )
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
        String type = objectManager.getType(client.getResourceType());

        return CONFIG_ITEM_STATUS.RESOURCE_TYPE.eq(type)
                .and(CONFIG_ITEM_STATUS.RESOURCE_ID.eq(client.getResourceId()));
    }


    @Override
    public void setItemSourceVersion(String name, String sourceRevision) {
        log.info("Setting config [{}] to source version [{}]", name, sourceRevision);
        int updated = create()
                .update(CONFIG_ITEM)
                    .set(CONFIG_ITEM.SOURCE_VERSION, sourceRevision)
                .where(
                        CONFIG_ITEM.NAME.eq(name))
                .execute();

        if ( updated == 0 ) {
            create()
                .insertInto(CONFIG_ITEM, CONFIG_ITEM.NAME, CONFIG_ITEM.NAME)
                .values(name, sourceRevision)
                .execute();
        }
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
