package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceExposeMapRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public class ServiceExposeMapDaoImpl extends AbstractJooqDao implements ServiceExposeMapDao {

    @Inject
    ObjectManager objectManager;

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ServiceDiscoveryService sdService;

    @Override
    public List<? extends Instance> listActiveServiceInstances(long serviceId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .where(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getActiveServiceInstance(long serviceId, String instanceName) {
        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .where(INSTANCE.REMOVED.isNull().and(INSTANCE.NAME.eq(instanceName)))
                .fetchInto(InstanceRecord.class);
        if (instances.isEmpty()) {
            return null;
        }
        return instances.get(0);
    }

    @Override
    public Instance createServiceInstance(Map<Object, Object> properties, Service service, String instanceName) {
        Map<String, Object> props = objectManager.convertToPropertiesFor(Instance.class,
                properties);
        final Instance instance = objectManager.create(Instance.class, props);
        objectManager.create(ServiceExposeMap.class, SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId());
        DeferredUtils.nest(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, instance, null);
                return null;
            }
        });
        return objectManager.reload(instance);
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
                .where(INSTANCE.REMOVED.isNull())
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends ServiceExposeMap> listServiceRemovedInstancesMaps(long serviceId) {
        return create()
                .select(SERVICE_EXPOSE_MAP.fields())
                .from(SERVICE_EXPOSE_MAP)
                .join(INSTANCE)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
                .where(INSTANCE.STATE.eq(CommonStatesConstants.REMOVED))
                .fetchInto(ServiceExposeMapRecord.class);
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
}
