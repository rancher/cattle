package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.tables.records.InstanceLinkRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceConsumeMapRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;

import java.util.List;

import javax.inject.Inject;

public class ServiceConsumeMapDaoImpl extends AbstractJooqDao implements ServiceConsumeMapDao {

    @Inject
    ObjectManager objectManager;

    @Override
    public ServiceConsumeMap findMapToRemove(long serviceId, long consumedServiceId) {
        List<ServiceConsumeMap> maps = objectManager.find(ServiceConsumeMap.class,
                SERVICE_CONSUME_MAP.SERVICE_ID,
                serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId);
        for (ServiceConsumeMap map : maps) {
            if (map != null && (map.getRemoved() == null || map.getState().equals(CommonStatesConstants.REMOVING))) {
                return map;
            }
        }

        return null;
    }

    @Override
    public ServiceConsumeMap findNonRemovedMap(long serviceId, long consumedServiceId, String linkName) {
        ServiceConsumeMap map = null;
        if (linkName == null) {
            map = objectManager.findOne(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId, SERVICE_CONSUME_MAP.REMOVED,
                    null);
        } else {
            map = objectManager.findOne(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId, SERVICE_CONSUME_MAP.NAME,
                    linkName, SERVICE_CONSUME_MAP.REMOVED,
                    null);
        }
        if (map != null && !map.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
            return map;
        }
        return null;
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumedServices(long serviceId) {
        return create()
                .selectFrom(SERVICE_CONSUME_MAP)
                .where(
                        SERVICE_CONSUME_MAP.SERVICE_ID.eq(serviceId)
                                .and(SERVICE_CONSUME_MAP.REMOVED.isNull())).fetchInto(ServiceConsumeMapRecord.class);
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumedServicesForInstance(long instanceId) {
        return create()
                .select(SERVICE_CONSUME_MAP.fields())
                .from(SERVICE_CONSUME_MAP)
                .leftOuterJoin(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE_CONSUME_MAP.SERVICE_ID))
                .where(
                        SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instanceId)
                                .and(SERVICE_CONSUME_MAP.REMOVED.isNull())
                                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
                .fetchInto(ServiceConsumeMapRecord.class);
    }

    @Override
    public List<? extends InstanceLink> findServiceBasedInstanceLinks(long instanceId) {
        return create()
                .select(INSTANCE_LINK.fields())
                .from(INSTANCE_LINK)
                .where(INSTANCE_LINK.INSTANCE_ID.eq(instanceId)
                        .and(INSTANCE_LINK.SERVICE_CONSUME_MAP_ID.isNotNull())
                        .and(INSTANCE_LINK.REMOVED.isNull()))
                .fetchInto(InstanceLinkRecord.class);
    }

    @Override
    public Instance findOneInstanceForService(long serviceId) {
        Instance last = null;

        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.REMOVED.isNull()
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
                .orderBy(INSTANCE.CREATED.desc())
                .fetchInto(InstanceRecord.class);

        for (Instance instance : instances) {
            last = instance;
            if (last.getFirstRunning() != null) {
                return last;
            }
        }

        return last;
    }

    @Override
    public List<String> findInstanceNamesForService(long serviceId) {
        return create()
                .select(INSTANCE.NAME)
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(INSTANCE.REMOVED.isNull()
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId)))
                .orderBy(INSTANCE.NAME.asc())
                .fetch(INSTANCE.NAME);
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumedMapsToRemove(long serviceId) {
        return create()
                .selectFrom(SERVICE_CONSUME_MAP)
                .where(
                        SERVICE_CONSUME_MAP.SERVICE_ID.eq(serviceId)
                                .and((SERVICE_CONSUME_MAP.REMOVED.isNull().
                                        or(SERVICE_CONSUME_MAP.STATE.eq(CommonStatesConstants.REMOVING))))).
                fetchInto(ServiceConsumeMapRecord.class);
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumingServices(long serviceId) {
        return create()
                .selectFrom(SERVICE_CONSUME_MAP)
                .where(
                        SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID.eq(serviceId)
                                .and(SERVICE_CONSUME_MAP.REMOVED.isNull())).fetchInto(ServiceConsumeMapRecord.class);
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumingMapsToRemove(long serviceId) {
        return create()
                .selectFrom(SERVICE_CONSUME_MAP)
                .where(
                        SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID.eq(serviceId)
                                .and((SERVICE_CONSUME_MAP.REMOVED.isNull().
                                        or(SERVICE_CONSUME_MAP.STATE.eq(CommonStatesConstants.REMOVING))))).
                fetchInto(ServiceConsumeMapRecord.class);
    }

}
