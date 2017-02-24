package io.cattle.platform.servicediscovery.dao.impl;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceExposeMapRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.config.Configuration;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.dao.ServiceExposeMapDao;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Condition;
import org.jooq.impl.DSL;

public class ServiceExposeMapDaoImpl extends AbstractJooqDao implements ServiceExposeMapDao {
    @Inject
    ObjectManager objectManager;
    @Inject
    LockManager lockManager;

    Configuration lockingConfiguration;

    @Override
    public Pair<Instance, ServiceExposeMap> createServiceInstance(final Map<String, Object> properties,
            final Service service, final ServiceRecord record) {
        record.attach(lockingConfiguration);
        record.setCreateIndex((record.getCreateIndex() == null ? 0 : record.getCreateIndex()) + 1);
        record.update();
        properties.put(InstanceConstants.FIELD_CREATE_INDEX, record.getCreateIndex());
        properties.put(ServiceConstants.FIELD_SYSTEM, ServiceConstants.isSystem(service));
        final Instance instance = objectManager.create(Instance.class, properties);
        ServiceExposeMap exposeMap = createServiceInstanceMap(service, instance, true);
        return Pair.of(instance, exposeMap);
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
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING,
                                InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                        .and(SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull().or(
                                SERVICE_EXPOSE_MAP.DNS_PREFIX.in(ServiceDiscoveryUtil
                                        .getServiceLaunchConfigNames(service)))))
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
                                SERVICE_EXPOSE_MAP.DNS_PREFIX.in(ServiceDiscoveryUtil
                                        .getServiceLaunchConfigNames(service)))))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<Pair<Instance, ServiceExposeMap>> listDeploymentUnitInstancesExposeMaps(Service service, DeploymentUnit unit) {
        MultiRecordMapper<Pair<Instance, ServiceExposeMap>> mapper = new MultiRecordMapper<Pair<Instance, ServiceExposeMap>>() {
            @Override
            protected Pair<Instance, ServiceExposeMap> map(List<Object> input) {
                return Pair.of((Instance) input.get(0), (ServiceExposeMap) input.get(1));
            }
        };

        InstanceTable instance = mapper.add(INSTANCE);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);

        return create()
                .select(mapper.fields())
                .from(instance)
                .join(exposeMap)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID)
                        .and(instance.DEPLOYMENT_UNIT_ID.eq(unit.getId()))
                        .and(exposeMap.MANAGED.eq(true))
                        .and(instance.REMOVED.isNull()))
                        .and(instance.STATE.notIn(CommonStatesConstants.REMOVING,
                                CommonStatesConstants.REMOVED,
                                CommonStatesConstants.PURGED,
                        CommonStatesConstants.PURGING))
                        .and(exposeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(exposeMap.DNS_PREFIX.isNull().or(
                                exposeMap.DNS_PREFIX.in(ServiceDiscoveryUtil
                                        .getServiceLaunchConfigNames(service))))
                .fetch().map(mapper);
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
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING,
                        InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                .and(SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull())
                .and(DEPLOYMENT_UNIT.STATE.notIn(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING))
                .and(DEPLOYMENT_UNIT.REMOVED.isNull())
                .fetchOne(0, Integer.class);
    }

    @Override
    public List<? extends Instance> listServiceManagedInstances(Service service, String launchConfigName) {
        Condition condition = null;
        if (launchConfigName == null || launchConfigName.equals(service.getName())
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            condition = SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull();
        } else {
            condition = SERVICE_EXPOSE_MAP.DNS_PREFIX.eq(launchConfigName);
        }
        List<? extends Instance> instances = create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.MANAGED.eq(true))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING,
                                InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING))
                        .and(condition))
                .fetchInto(InstanceRecord.class);

        List<? extends Host> validHosts = create()
                .select(HOST.fields())
                .from(HOST)
                .where(HOST.ACCOUNT_ID.eq(service.getAccountId()))
                .and(HOST.STATE.notIn(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING,
                        CommonStatesConstants.PURGED, CommonStatesConstants.PURGING))
                .and(HOST.ACCOUNT_ID.eq(service.getAccountId()))
                        .fetchInto(HostRecord.class);
        @SuppressWarnings("unchecked")
        List<Long> validHostIds = (List<Long>) CollectionUtils.collect(validHosts,
                TransformerUtils.invokerTransformer("getId"));

        List<Instance> toReturn = new ArrayList<>();
        for (Instance instance : instances) {
            Long hostId = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_HOST_ID);
            if (hostId == null || validHostIds.contains(hostId)) {
                toReturn.add(instance);
            }
        }
        return toReturn;
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
                .and(SERVICE.STATE.in(ServiceDiscoveryUtil.getServiceActiveStates()))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public Host getHostForInstance(long instanceId) {
        List<? extends Host> results = create()
                .select(HOST.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instanceId))
                .fetchInto(HostRecord.class);
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
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
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(false))
                        .and(SERVICE_EXPOSE_MAP.STATE.notIn(CommonStatesConstants.REQUESTED,
                                CommonStatesConstants.REMOVED,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(ServiceExposeMapRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesToUpgrade(Service service, String launchConfigName, String toVersion) {
        Condition condition = null;
        if (launchConfigName == null || launchConfigName.equals(service.getName())
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            condition = SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull();
        } else {
            condition = SERVICE_EXPOSE_MAP.DNS_PREFIX.eq(launchConfigName);
        }

        // add all managed
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.MANAGED.eq(true))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING)).and(condition)
                        .and(INSTANCE.VERSION.ne(toVersion)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> getUpgradedUnmanagedInstances(Service service, String launchConfigName,
            String toVersion) {
        Condition condition1 = null;
        if (launchConfigName == null || launchConfigName.equals(service.getName())
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            condition1 = SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull();
        } else {
            condition1 = SERVICE_EXPOSE_MAP.DNS_PREFIX.eq(launchConfigName);
        }

        Condition condition2 = SERVICE_EXPOSE_MAP.MANAGED.eq(false).and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true));

        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING)).and(condition1)
                        .and(condition2)
                        .and(INSTANCE.VERSION.eq(toVersion)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesToCleanup(Service service, String launchConfigName, String toVersion) {
        Condition condition = null;
        if (launchConfigName == null || launchConfigName.equals(service.getName())
                || launchConfigName.equals(ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME)) {
            condition = SERVICE_EXPOSE_MAP.DNS_PREFIX.isNull();
        } else {
            condition = SERVICE_EXPOSE_MAP.DNS_PREFIX.eq(launchConfigName);
        }
        // add unmanaged and marked for upgrade
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .and(SERVICE_EXPOSE_MAP.MANAGED.eq(false))
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true))
                        .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.PURGING, CommonStatesConstants.PURGED,
                                CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING)).and(condition)
                        .and(INSTANCE.VERSION.ne(toVersion)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesSetForUpgrade(long serviceId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .where(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(serviceId))
                .and(SERVICE_EXPOSE_MAP.MANAGED.eq(false))
                .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true))
                .and(SERVICE_EXPOSE_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                        CommonStatesConstants.ACTIVE, CommonStatesConstants.REQUESTED))
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
}
