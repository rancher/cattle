package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceConsumeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceConsumeMapRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.RecordHandler;

public class ServiceConsumeMapDaoImpl extends AbstractJooqDao implements ServiceConsumeMapDao {

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    LockManager lockManager;
    TransactionDelegate transaction;

    public ServiceConsumeMapDaoImpl(Configuration configuration, ObjectManager objectManager, ObjectProcessManager objectProcessManager,
            LockManager lockManager, TransactionDelegate transaction) {
        super(configuration);
        this.objectManager = objectManager;
        this.objectProcessManager = objectProcessManager;
        this.lockManager = lockManager;
        this.transaction = transaction;
    }

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
        List<? extends ServiceConsumeMap> maps = new ArrayList<>();
        if (linkName == null) {
            maps = objectManager.find(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId, SERVICE_CONSUME_MAP.REMOVED,
                    null);
        } else {
            maps = objectManager.find(ServiceConsumeMap.class,
                    SERVICE_CONSUME_MAP.SERVICE_ID,
                    serviceId, SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, consumedServiceId, SERVICE_CONSUME_MAP.NAME,
                    linkName, SERVICE_CONSUME_MAP.REMOVED,
                    null);
        }

        for (ServiceConsumeMap m : maps) {
            if (m.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
                continue;
            }
            map = m;
        }
        return map;
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumedServices(long serviceId) {
        return create()
                .selectFrom(SERVICE_CONSUME_MAP)
                .where(
                        SERVICE_CONSUME_MAP.SERVICE_ID.eq(serviceId)
                                .and(SERVICE_CONSUME_MAP.REMOVED.isNull())
                                .and(SERVICE_CONSUME_MAP.STATE.notIn(CommonStatesConstants.REMOVING)))
                .fetchInto(ServiceConsumeMapRecord.class);
    }

    @Override
    public List<? extends ServiceConsumeMap> findConsumedServicesForInstance(long instanceId, List<String> kinds) {
        return create()
                .select(SERVICE_CONSUME_MAP.fields())
                .from(SERVICE_CONSUME_MAP)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE_CONSUME_MAP.SERVICE_ID))
                .join(SERVICE)
                    .on(SERVICE.ID.eq(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID))
                .where(
                        SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instanceId)
                                .and(SERVICE.KIND.in(kinds))
                                .and(SERVICE_CONSUME_MAP.REMOVED.isNull())
                                //Don't include yourself
                                .and(SERVICE_CONSUME_MAP.SERVICE_ID.ne(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID))
                                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
                .fetchInto(ServiceConsumeMapRecord.class);
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
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
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
    public ServiceConsumeMap createServiceLink(final Service service, final ServiceLink serviceLink) {
        return transaction.doInTransactionResult(() -> {
            return lockManager.lock(new ServiceLinkLock(service.getId(), serviceLink.getServiceId()),
                    new LockCallback<ServiceConsumeMap>() {
                @Override
                        public ServiceConsumeMap doWithLock() {
                    return createServiceLinkImpl(service, serviceLink);
                }
            });
        });
    }

    protected ServiceConsumeMap createServiceLinkImpl(Service service, ServiceLink serviceLink) {
        Service linkFrom = objectManager.reload(service);
        if (linkFrom == null || linkFrom.getRemoved() != null
                || linkFrom.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
            return null;
        }
        Service linkTo = objectManager.loadResource(Service.class, serviceLink.getServiceId());
        if (linkTo == null || linkTo.getRemoved() != null
                || linkTo.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING)) {
            return null;
        }

        ServiceConsumeMap map = findNonRemovedMap(service.getId(), serviceLink.getServiceId(),
                serviceLink.getName());

        if (map == null) {
            Map<Object,Object> properties = CollectionUtils.asMap(
                    (Object)SERVICE_CONSUME_MAP.SERVICE_ID,
                    service.getId(), SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, serviceLink.getServiceId(),
                    SERVICE_CONSUME_MAP.ACCOUNT_ID, service.getAccountId(),
                    SERVICE_CONSUME_MAP.NAME, serviceLink.getName());

            map = objectManager.create(ServiceConsumeMap.class, objectManager.convertToPropertiesFor(ServiceConsumeMap.class,
                    properties));
        }

        if (map.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleProcessInstance(ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                    map, null);
        }

        return map;
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
    public List<? extends ServiceConsumeMap> findConsumingMapsToRemove(long serviceId) {
        return create()
                .selectFrom(SERVICE_CONSUME_MAP)
                .where(
                        SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID.eq(serviceId)
                                .and((SERVICE_CONSUME_MAP.REMOVED.isNull().
                                        or(SERVICE_CONSUME_MAP.STATE.eq(CommonStatesConstants.REMOVING))))).
                fetchInto(ServiceConsumeMapRecord.class);
    }

    @Override
    public Map<Long, Long> findConsumedServicesIdsToStackIdsFromOtherAccounts(long accountId) {
        final Map<Long, Long> result = new HashMap<>();
        create().select(SERVICE.ID, SERVICE.STACK_ID)
        .from(SERVICE)
        .join(SERVICE_CONSUME_MAP)
        .on(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID.eq(SERVICE.ID))
        .where(SERVICE_CONSUME_MAP.ACCOUNT_ID.eq(accountId)
                        .and(SERVICE_CONSUME_MAP.REMOVED.isNull())
                        .and(SERVICE.REMOVED.isNull())
                        .and(SERVICE.ACCOUNT_ID.ne(accountId)))
                .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                    @Override
                    public void next(Record2<Long, Long> record) {
                        Long serviceId = record.getValue(SERVICE.ID);
                        Long stackId = record.getValue(SERVICE.STACK_ID);
                        result.put(serviceId, stackId);
                    }
                });
        return result;
    }

    @Override
    public Map<Long, Long> findConsumedByServicesIdsToStackIdsFromOtherAccounts(long accountId) {
        List<Long> serviceIds =  Arrays.asList(create().select(SERVICE_CONSUME_MAP.SERVICE_ID)
                .from(SERVICE_CONSUME_MAP)
                .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID))
                .where(SERVICE.ACCOUNT_ID.eq(accountId)
                        .and(SERVICE_CONSUME_MAP.REMOVED.isNull())
                        .and(SERVICE.REMOVED.isNull())
                        .and(SERVICE_CONSUME_MAP.ACCOUNT_ID.ne(accountId)))
                .fetch().intoArray(SERVICE_CONSUME_MAP.SERVICE_ID));

        Map<Long, Long> result = create().select(SERVICE.ID, SERVICE.STACK_ID)
                .from(SERVICE)
                .where(SERVICE.ID.in(serviceIds)
                        .and(SERVICE.REMOVED.isNull()))
                .fetch().intoMap(SERVICE.ID, SERVICE.STACK_ID);


        return result;
    }

    @Override
    public void removeServiceLink(Service service, ServiceLink serviceLink) {
        ServiceConsumeMap map = findMapToRemove(service.getId(), serviceLink.getServiceId());

        if (map != null) {
            objectProcessManager.scheduleProcessInstance(ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                    map, null);
        }
    }
}
