package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceExposeMapRecord;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceExposeMapDaoImpl extends AbstractJooqDao implements ServiceExposeMapDao {
    @Inject
    ObjectManager objectManager;

    @Override
    @SuppressWarnings("unchecked")
    public ServiceExposeMap createServiceInstanceMap(Service service, final Instance instance, boolean managed) {
        Map<String, String> instanceLabels = DataAccessor.fields(instance)
                .withKey(InstanceConstants.FIELD_LABELS).withDefault(Collections.EMPTY_MAP).as(Map.class);
        String dnsPrefix = instanceLabels
                .get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
        if (ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equalsIgnoreCase(dnsPrefix)) {
            dnsPrefix = null;
        }

        ServiceExposeMap map = createServiceInstanceMap(service, instance, managed, dnsPrefix);
        return map;
    }

    @Override
    public ServiceExposeMap createServiceInstanceMap(Service service, final Instance instance, boolean managed,
            String dnsPrefix) {
        ServiceExposeMap map = objectManager.findAny(ServiceExposeMap.class,
                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId(), SERVICE_EXPOSE_MAP.REMOVED, null,
                SERVICE_EXPOSE_MAP.STATE, new io.github.ibuildthecloud.gdapi.condition.Condition(
                        ConditionType.NE, CommonStatesConstants.REMOVING));
        if (map == null) {
            map = objectManager.create(ServiceExposeMap.class, SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                    SERVICE_EXPOSE_MAP.SERVICE_ID, service.getId(), SERVICE_EXPOSE_MAP.ACCOUNT_ID,
                    service.getAccountId(), SERVICE_EXPOSE_MAP.DNS_PREFIX, dnsPrefix, SERVICE_EXPOSE_MAP.MANAGED,
                    managed);
        }
        return map;
    }


    @Override
    public List<? extends Instance> listServiceManagedInstances(Service service) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.MANAGED.eq(true))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.REMOVING,
                                InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                        .and(INSTANCE.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull().or(
                                SERVICE_EXPOSE_MAP.DNS_PREFIX.in(ServiceUtil
                                        .getLaunchConfigNames(service)))))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends ServiceExposeMap> getUnmanagedServiceInstanceMapsToRemove(long serviceId) {
        return create()
                .select(SERVICE_EXPOSE_MAP.fields())
                .from(SERVICE_EXPOSE_MAP)
                .join(INSTANCE)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                        .and(SERVICE_EXPOSE_MAP.MANAGED.eq(false))
                        .and(SERVICE_EXPOSE_MAP.STATE.notIn(CommonStatesConstants.REQUESTED,
                                CommonStatesConstants.REMOVED,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(ServiceExposeMapRecord.class);
    }

    @SuppressWarnings("unchecked")
    protected Map<String, String> getRevisionConfigNameToVersions(Service service, Revision revision) {
        Map<String, String> versions = new HashMap<>();
        Map<String, Object> revisionConfig = CollectionUtils.toMap(DataAccessor.field(
                revision, InstanceConstants.FIELD_REVISION_CONFIG, Object.class));
        Map<String, Object> configs = new HashMap<>();
        if (revisionConfig.get(ServiceConstants.FIELD_LAUNCH_CONFIG) != null) {
            configs.put(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME,
                    revisionConfig.get(ServiceConstants.FIELD_LAUNCH_CONFIG));
        }
        if (revisionConfig.get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS) != null) {
            for (Object sc : (List<Object>) revisionConfig
                    .get(ServiceConstants.FIELD_SECONDARY_LAUNCH_CONFIGS)) {
                Map<String, Object> lc = CollectionUtils.toMap(sc);
                configs.put(lc.get("name").toString(), lc);
            }
        }
        for (String lcName : configs.keySet()) {
            Map<String, Object> lc = CollectionUtils.toMap(configs.get(lcName));
            if (lc.containsKey(ServiceConstants.FIELD_VERSION)) {
                versions.put(lcName, lc.get(ServiceConstants.FIELD_VERSION).toString());
            }
        }
        return versions;
    }

    @Override
    public void deleteServiceExposeMaps(Instance instance) {
        create().deleteFrom(SERVICE_EXPOSE_MAP)
            .where(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instance.getId()))
            .execute();
    }

}
