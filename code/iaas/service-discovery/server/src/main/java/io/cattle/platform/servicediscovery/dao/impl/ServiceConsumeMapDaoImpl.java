package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.ServiceConsumeMap;
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
        if (linkName == null) {
            return objectManager.findOne(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId, SERVICE_CONSUME_MAP.REMOVED,
                    null);
        } else {
            return objectManager.findOne(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId, SERVICE_CONSUME_MAP.NAME,
                    linkName, SERVICE_CONSUME_MAP.REMOVED,
                    null);
        }
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
