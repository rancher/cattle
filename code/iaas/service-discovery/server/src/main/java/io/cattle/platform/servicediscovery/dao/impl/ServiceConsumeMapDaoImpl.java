package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.INSTANCE_LINK;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.addon.LoadBalancerServiceLink;
import io.cattle.platform.core.addon.ServiceLink;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.tables.records.InstanceLinkRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceConsumeMapRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceConsumeMapDao;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class ServiceConsumeMapDaoImpl extends AbstractJooqDao implements ServiceConsumeMapDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    JsonMapper jsonMapper;

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
    public List<? extends ServiceConsumeMap> findConsumedServicesForInstance(long instanceId, String kind) {
        return create()
                .select(SERVICE_CONSUME_MAP.fields())
                .from(SERVICE_CONSUME_MAP)
                .join(SERVICE_EXPOSE_MAP)
                    .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE_CONSUME_MAP.SERVICE_ID))
                .join(SERVICE)
                    .on(SERVICE.ID.eq(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID))
                .where(
                        SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instanceId)
                                .and(SERVICE.KIND.eq(kind))
                                .and(SERVICE_CONSUME_MAP.REMOVED.isNull())
                                //Don't include yourself
                                .and(SERVICE_CONSUME_MAP.SERVICE_ID.ne(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID))
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
    public ServiceConsumeMap createServiceLink(Service service, ServiceLink serviceLink) {
        ServiceConsumeMap map = findNonRemovedMap(service.getId(), serviceLink.getServiceId(),
                serviceLink.getName());

        boolean update = false;
        if (map == null) {
            Map<Object,Object> properties = CollectionUtils.asMap(
                    (Object)SERVICE_CONSUME_MAP.SERVICE_ID,
                    service.getId(), SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID, serviceLink.getServiceId(),
                    SERVICE_CONSUME_MAP.ACCOUNT_ID, service.getAccountId(),
                    SERVICE_CONSUME_MAP.NAME, serviceLink.getName());

            if (serviceLink instanceof LoadBalancerServiceLink) {
                  properties.put(LoadBalancerConstants.FIELD_LB_TARGET_PORTS,
                          ((LoadBalancerServiceLink) serviceLink).getPorts());
            }

            map = objectManager.create(ServiceConsumeMap.class, objectManager.convertToPropertiesFor(ServiceConsumeMap.class,
                    properties));
        } else {
            if (service.getKind()
                    .equalsIgnoreCase(ServiceDiscoveryConstants.KIND.LOADBALANCERSERVICE.name())) {
                LoadBalancerServiceLink newLbServiceLink = (LoadBalancerServiceLink) serviceLink;
                List<? extends String> newPorts = newLbServiceLink.getPorts() != null ? newLbServiceLink.getPorts()
                        : new ArrayList<String>();
                DataUtils.getWritableFields(map).put(LoadBalancerConstants.FIELD_LB_TARGET_PORTS, newPorts);
                objectManager.persist(map);
                update = true;
            }
        }

        if (map.getState().equalsIgnoreCase(CommonStatesConstants.REQUESTED)) {
            objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                    map, null);
        }

        if (update) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, map, null);
        }

        return map;
    }

    @Override
    public List<ServiceConsumeMap> createServiceLinks(List<ServiceLink> serviceLinks) {
        List<ServiceConsumeMap> result = new ArrayList<>();

        for (ServiceLink serviceLink : serviceLinks) {
            Service service = objectManager.loadResource(Service.class, serviceLink.getConsumingServiceId());
            if (service == null) {
                continue;
            }

            result.add(createServiceLink(service, serviceLink));
        }

        return result;
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
