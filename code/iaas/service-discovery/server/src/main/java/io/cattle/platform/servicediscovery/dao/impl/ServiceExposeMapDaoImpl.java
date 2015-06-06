package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceExposeMapRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;

public class ServiceExposeMapDaoImpl extends AbstractJooqDao implements ServiceExposeMapDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    GenericMapDao mapDao;


    @Override
    public Pair<Instance, ServiceExposeMap> createServiceInstance(Map<String, Object> properties, Service service,
            String instanceName) {
        final Instance instance = objectManager.create(Instance.class, properties);
        ServiceExposeMap exposeMap = createServiceInstanceMap(service, instance);
        return Pair.of(instance, exposeMap);
    }

    @Override
    public ServiceExposeMap createServiceInstanceMap(Service service, final Instance instance) {
            return objectManager.create(ServiceExposeMap.class, SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                    SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId(), SERVICE_EXPOSE_MAP.ACCOUNT_ID,
                service.getAccountId());
    }

    @Override
    public List<? extends Instance> listServiceInstances(long serviceId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public void updateServiceName(Service service) {
        List<? extends Instance> instances = updateInstanceNames(service);
        for (Instance instance : instances) {
            objectManager.persist(instance);
        }
    }

    private List<? extends Instance> updateInstanceNames(Service service) {
        List<? extends Instance> instances = listServiceInstances(service.getId());
        int i = 1;
        for (Instance instance : instances) {
            instance.setName(sdService.generateServiceInstanceName(service, i));
            i++;
        }
        return instances;
    }

    @Override
    public void updateEnvironmentName(Environment env) {
        List<? extends Service> services = objectManager.mappedChildren(env, Service.class);
        List<Instance> instances = new ArrayList<>();
        for (Service service : services) {
            if (service.getRemoved() == null && !service.getState().equalsIgnoreCase(CommonStatesConstants.REMOVED)
                    && !service.getState().equalsIgnoreCase(CommonStatesConstants.REMOVING))
                instances.addAll(updateInstanceNames(service));
        }
        for (Instance instance : instances) {
            objectManager.persist(instance);
        }
    }

    @Override
    public List<? extends ServiceExposeMap> getNonRemovedServiceInstanceMap(long serviceId) {
        return create()
                .select(SERVICE_EXPOSE_MAP.fields())
                .from(SERVICE_EXPOSE_MAP)
                .join(INSTANCE)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull().and(
                                SERVICE_EXPOSE_MAP.STATE.notIn(CommonStatesConstants.REMOVED,
                                        CommonStatesConstants.REMOVING))))
                .fetchInto(ServiceExposeMapRecord.class);
    }

    @Override
    public List<Service> collectSidekickServices(Service initialService, Map<String, String> initialSvcLabels) {
        List<Service> servicesToCollect = new ArrayList<>();
        if (initialSvcLabels == null) {
            initialSvcLabels = sdService.getServiceLabels(initialService);
        }
        String initialSvcSidekickLabel = initialSvcLabels.
                get(ServiceDiscoveryConstants.LABEL_SERVICE_SIDEKICK);

        // add itself
        servicesToCollect.add(initialService);

        // add sidekick services
        if (initialSvcSidekickLabel != null) {
            /*
             * Find all sidekick services. Just look at sidekick, not volumes-from or other things
             */
            List<? extends Service> services = sdService.listEnvironmentServices(initialService.getEnvironmentId());
            for (Service svc : services) {
                if (svc.getId().equals(initialService.getId())) {
                    continue;
                }

                Map<String, String> svcLabels = sdService.getServiceLabels(svc);
                String svcSidekickLabel = svcLabels.get(
                        ServiceDiscoveryConstants.LABEL_SERVICE_SIDEKICK) == null ? null
                        : svcLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_SIDEKICK);
                if (initialSvcSidekickLabel.equalsIgnoreCase(svcSidekickLabel)) {
                    servicesToCollect.add(svc);
                }
            }
        }
        return servicesToCollect;
    }

    @Override
    public void updateScale(List<Service> services, Integer scale) {
        for (Service service : services) {
            service = objectManager.reload(service);
            DataAccessor.fields(service).withKey(ServiceDiscoveryConstants.FIELD_SCALE)
                    .set(scale);
            service = objectManager.persist(service);
        }
    }

    @Override
    public ServiceExposeMap findInstanceExposeMap(Instance instance) {
        if (instance == null) {
            return null;
        }
        List<? extends ServiceExposeMap> instanceServiceMap = mapDao.findNonRemoved(ServiceExposeMap.class,
                Instance.class,
                instance.getId());
        if (instanceServiceMap.isEmpty()) {
            // not a service instance
            return null;
        }
        return instanceServiceMap.get(0);
    }

    @Override
    public ServiceExposeMap createIpToServiceMap(Service service, String ipAddress) {
        ServiceExposeMap map = getServiceIpExposeMap(service, ipAddress);
        if (map == null) {
            map = objectManager.create(ServiceExposeMap.class, SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId(),
                    SERVICE_EXPOSE_MAP.IP_ADDRESS, ipAddress, SERVICE_EXPOSE_MAP.ACCOUNT_ID,
                    service.getAccountId());
        }
        return map;
    }

    @Override
    public ServiceExposeMap getServiceIpExposeMap(Service service, String ipAddress) {
        return objectManager.findAny(ServiceExposeMap.class,
                SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId(),
                SERVICE_EXPOSE_MAP.IP_ADDRESS, ipAddress,
                SERVICE_EXPOSE_MAP.REMOVED, null);
    }

    @Override
    public List<? extends Service> getActiveServices(long accountId) {
        return create()
                .select(SERVICE.fields())
                .from(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .and(SERVICE.STATE.in(CommonStatesConstants.ACTIVE, CommonStatesConstants.ACTIVATING, CommonStatesConstants.UPDATING_ACTIVE))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends ServiceExposeMap> getNonRemovedServiceIpMaps(long serviceId) {
        return create()
                .select(SERVICE_EXPOSE_MAP.fields())
                .from(SERVICE_EXPOSE_MAP)
                .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()
                        .and(SERVICE_EXPOSE_MAP.STATE.notIn(CommonStatesConstants.REMOVED,
                                CommonStatesConstants.REMOVING))
                        .and(SERVICE_EXPOSE_MAP.IP_ADDRESS.isNotNull()))
                .fetchInto(ServiceExposeMapRecord.class);
    }
}
