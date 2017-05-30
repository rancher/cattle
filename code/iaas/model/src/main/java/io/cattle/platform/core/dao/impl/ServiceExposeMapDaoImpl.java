package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.ServiceExposeMapDao;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceExposeMapRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.db.jooq.config.Configuration;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class ServiceExposeMapDaoImpl extends AbstractJooqDao implements ServiceExposeMapDao {
    @Inject
    ObjectManager objectManager;

    Configuration lockingConfiguration;

    @Override
    public Pair<Instance, ServiceExposeMap> createServiceInstance(final Map<String, Object> properties, Long serviceId, boolean system) {
        Map<String, String> labels = CollectionUtils.toMap(properties.get(InstanceConstants.FIELD_LABELS));
        String dnsPrefix = labels.get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
        if (ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equalsIgnoreCase(dnsPrefix)) {
            dnsPrefix = null;
        }

        Long next = null;

        if (serviceId != null) {
            Long index = create().select(SERVICE.CREATE_INDEX)
                .from(SERVICE)
                .where(SERVICE.ID.eq(serviceId))
                .forUpdate()
                .fetchAny().value1();
            Condition cond = index == null ? SERVICE.CREATE_INDEX.isNull() : SERVICE.CREATE_INDEX.eq(index);
            next = index == null ? 0L : index+1;

            create().update(SERVICE)
                .set(SERVICE.CREATE_INDEX, next)
                .where(SERVICE.ID.eq(serviceId)
                        .and(cond))
                .execute();
        }

        properties.put(InstanceConstants.FIELD_CREATE_INDEX, next);
        properties.put(ServiceConstants.FIELD_SYSTEM, system);

        Instance instance = objectManager.create(Instance.class, properties);

        ServiceExposeMap map = null;

        if (serviceId != null ) {
            map = objectManager.create(ServiceExposeMap.class,
                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                SERVICE_EXPOSE_MAP.SERVICE_ID, serviceId,
                SERVICE_EXPOSE_MAP.ACCOUNT_ID, instance.getAccountId(),
                SERVICE_EXPOSE_MAP.DNS_PREFIX, dnsPrefix,
                SERVICE_EXPOSE_MAP.MANAGED, true);
        }

        return Pair.of(instance, map);
    }

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
    public List<? extends Instance> listServiceManagedInstancesAll(Service service) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.MANAGED.eq(true))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                                .and(SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull().or(
                                SERVICE_EXPOSE_MAP.DNS_PREFIX.in(ServiceUtil
                                        .getLaunchConfigNames(service)))))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<Pair<Instance, ServiceExposeMap>> listDeploymentUnitInstances(Service service, DeploymentUnit unit, boolean forCurrentRevision) {
        Revision revision = objectManager.loadResource(Revision.class, unit.getRevisionId());
        if (revision == null) {
            return new ArrayList<>();
        }

        MultiRecordMapper<Pair<Instance, ServiceExposeMap>> mapper = new MultiRecordMapper<Pair<Instance, ServiceExposeMap>>() {
            @Override
            protected Pair<Instance, ServiceExposeMap> map(List<Object> input) {
                return Pair.of((Instance) input.get(0), (ServiceExposeMap) input.get(1));
            }
        };

        InstanceTable instance = mapper.add(INSTANCE);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);

        List<Pair<Instance, ServiceExposeMap>> instances = create()
                .select(mapper.fields())
                .from(instance)
                .join(exposeMap)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID)
                        .and(instance.DEPLOYMENT_UNIT_ID.eq(unit.getId()))
                        .and(exposeMap.MANAGED.eq(true))
                        .and(instance.REMOVED.isNull()))
                .and(instance.REMOVED.isNull())
                .and(instance.STATE.notIn(CommonStatesConstants.REMOVING))
                .and(exposeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                        CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                .fetch().map(mapper);

        Map<String, String> currentVersions = getRevisionConfigNameToVersions(service, revision);
        List<Pair<Instance, ServiceExposeMap>> toReturn = new ArrayList<>();
        for (Pair<Instance, ServiceExposeMap> it : instances) {
            String lcName = it.getRight().getDnsPrefix() == null ? ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME : it
                    .getRight().getDnsPrefix();
            Instance i = it.getLeft();
            boolean configExists = currentVersions.containsKey(lcName);
            boolean versionMatch = currentVersions.containsKey(lcName) && currentVersions.get(lcName).equals(i.getVersion());
            if (forCurrentRevision) {
                if (configExists && versionMatch) {
                    toReturn.add(it);
                }
            } else {
                if (!configExists || !versionMatch) {
                    toReturn.add(it);
                }
            }
        }
        return toReturn;
    }

    @Override
    public Integer getCurrentScale(long serviceId) {
        return create()
                .select(DSL.count())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .join(DEPLOYMENT_UNIT)
                .on(DEPLOYMENT_UNIT.ID.eq(INSTANCE.DEPLOYMENT_UNIT_ID))
                        .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(false))
                        .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.REMOVING,
                        InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                .and(SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull())
                .and(DEPLOYMENT_UNIT.STATE.notIn(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING))
                .and(DEPLOYMENT_UNIT.REMOVED.isNull())
                .fetchOne(0, Integer.class);
    }


    @Override
    public List<? extends Service> getActiveServices(long accountId) {
        return create()
                .select(SERVICE.fields())
                .from(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .and(SERVICE.STATE.in(ServiceUtil.getServiceActiveStates()))
                .fetchInto(ServiceRecord.class);
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

    @Override
    public List<? extends Instance> getServiceInstancesSetForUpgrade(long serviceId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true))
                .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                        CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.ne(CommonStatesConstants.REMOVING))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> getDeploymentUnitInstancesSetForUpgrade(DeploymentUnit unit) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(DEPLOYMENT_UNIT)
                .on(DEPLOYMENT_UNIT.ID.eq(INSTANCE.DEPLOYMENT_UNIT_ID))
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(DEPLOYMENT_UNIT.ID.eq(unit.getId()))
                .and(SERVICE_EXPOSE_MAP.MANAGED.eq(false))
                .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true))
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.ne(CommonStatesConstants.REMOVING))
                .fetchInto(InstanceRecord.class);
    }

    public Configuration getLockingConfiguration() {
        return lockingConfiguration;
    }

    public void setLockingConfiguration(Configuration lockingConfiguration) {
        this.lockingConfiguration = lockingConfiguration;
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
}
