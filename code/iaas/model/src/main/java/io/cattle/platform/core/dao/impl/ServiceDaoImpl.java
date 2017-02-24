package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.CertificateTable.*;
import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceRevisionTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceRevision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.records.DeploymentUnitRecord;
import io.cattle.platform.core.model.tables.records.HealthcheckInstanceRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.model.tables.records.StackRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Record6;
import org.jooq.RecordHandler;

@Named
public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {

    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    LockManager lockManager;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public Service getServiceByExternalId(Long accountId, String externalId) {
        return create().selectFrom(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .and(SERVICE.EXTERNAL_ID.eq(externalId))
                .fetchAny();
    }

    @Override
    public ServiceIndex createServiceIndex(Service service, String launchConfigName, String serviceIndex) {
        ServiceIndex serviceIndexObj = objectManager.findAny(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID,
                service.getId(),
                SERVICE_INDEX.LAUNCH_CONFIG_NAME, launchConfigName, SERVICE_INDEX.SERVICE_INDEX_, serviceIndex,
                SERVICE_INDEX.REMOVED, null);
        if (serviceIndexObj == null) {
            serviceIndexObj = objectManager.create(ServiceIndex.class, SERVICE_INDEX.SERVICE_ID,
                    service.getId(),
                    SERVICE_INDEX.LAUNCH_CONFIG_NAME, launchConfigName, SERVICE_INDEX.SERVICE_INDEX_, serviceIndex);
        }
        return serviceIndexObj;
    }

    @Override
    public Service getServiceByServiceIndexId(long serviceIndexId) {
        Record record = create()
                .select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_INDEX).on(SERVICE.ID.eq(SERVICE_INDEX.SERVICE_ID))
                .where(SERVICE_INDEX.ID.eq(serviceIndexId))
                .fetchAny();

        return record == null ? null : record.into(Service.class);
    }

    @Override
    public boolean isServiceManagedInstance(Instance instance) {
        return instance.getServiceId() != null;
    }

    @Override
    public Map<Long, List<Object>> getServicesForInstances(List<Long> ids, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();
        create().select(SERVICE_EXPOSE_MAP.INSTANCE_ID, SERVICE_EXPOSE_MAP.SERVICE_ID)
            .from(SERVICE_EXPOSE_MAP)
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .where(SERVICE_EXPOSE_MAP.REMOVED.isNull()
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE_EXPOSE_MAP.INSTANCE_ID.in(ids)))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long serviceId = record.getValue(SERVICE_EXPOSE_MAP.SERVICE_ID);
                    Long instanceId = record.getValue(SERVICE_EXPOSE_MAP.INSTANCE_ID);
                    List<Object> list = result.get(instanceId);
                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(instanceId, list);
                    }
                    list.add(idFormatter.formatId("service", serviceId));
                }
            });

        return result;
    }

    @Override
    public Map<Long, List<Object>> getInstances(List<Long> ids, final IdFormatter idFormatter) {
        final Map<Long, List<Object>> result = new HashMap<>();
        create().select(SERVICE_EXPOSE_MAP.INSTANCE_ID, SERVICE_EXPOSE_MAP.SERVICE_ID)
            .from(SERVICE_EXPOSE_MAP)
            .join(INSTANCE)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .where(SERVICE_EXPOSE_MAP.REMOVED.isNull()
                    .and(INSTANCE.REMOVED.isNull())
                    .and(SERVICE_EXPOSE_MAP.SERVICE_ID.in(ids))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull()))
            .fetchInto(new RecordHandler<Record2<Long, Long>>() {
                @Override
                public void next(Record2<Long, Long> record) {
                    Long serviceId = record.getValue(SERVICE_EXPOSE_MAP.SERVICE_ID);
                    Long instanceId = record.getValue(SERVICE_EXPOSE_MAP.INSTANCE_ID);
                    List<Object> list = result.get(serviceId);
                    if (list == null) {
                        list = new ArrayList<>();
                        result.put(serviceId, list);
                    }
                    list.add(idFormatter.formatId(InstanceConstants.TYPE, instanceId));
                }
            });
        return result;
    }

    @Override
    public Map<Long, List<ServiceLink>> getServiceLinks(List<Long> ids) {
        final Map<Long, List<ServiceLink>> result = new HashMap<>();
        create().select(SERVICE_CONSUME_MAP.NAME, SERVICE_CONSUME_MAP.SERVICE_ID, SERVICE.ID,
                SERVICE.NAME, STACK.ID, STACK.NAME)
            .from(SERVICE_CONSUME_MAP)
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_CONSUME_MAP.CONSUMED_SERVICE_ID))
            .join(STACK)
                .on(STACK.ID.eq(SERVICE.STACK_ID))
            .where(SERVICE_CONSUME_MAP.SERVICE_ID.in(ids)
                    .and(SERVICE_CONSUME_MAP.REMOVED.isNull()))
            .fetchInto(new RecordHandler<Record6<String, Long, Long, String, Long, String>>(){
                @Override
                public void next(Record6<String, Long, Long, String, Long, String> record) {
                    Long serviceId = record.getValue(SERVICE_CONSUME_MAP.SERVICE_ID);
                    List<ServiceLink> links = result.get(serviceId);
                    if (links == null) {
                        links = new ArrayList<>();
                        result.put(serviceId, links);
                    }
                    links.add(new ServiceLink(
                            record.getValue(SERVICE_CONSUME_MAP.NAME),
                            record.getValue(SERVICE.NAME),
                            record.getValue(SERVICE.ID),
                            record.getValue(STACK.ID),
                            record.getValue(STACK.NAME)));
                }
            });
        return result;
    }

    @Override
    public List<Certificate> getLoadBalancerServiceCertificates(Service lbService) {
        List<? extends Long> certIds = DataAccessor.fields(lbService)
                .withKey(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, Long.class);
        Long defaultCertId = DataAccessor.fieldLong(lbService, LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
        List<Long> allCertIds = new ArrayList<>();
        allCertIds.addAll(certIds);
        allCertIds.add(defaultCertId);
        return create()
                .select(CERTIFICATE.fields())
                .from(CERTIFICATE)
                .where(CERTIFICATE.REMOVED.isNull())
                .and(CERTIFICATE.ID.in(allCertIds))
                .fetchInto(Certificate.class);
    }

    @Override
    public Certificate getLoadBalancerServiceDefaultCertificate(Service lbService) {
        Long defaultCertId = DataAccessor.fieldLong(lbService, LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
        List<? extends Certificate> certs = create()
                .select(CERTIFICATE.fields())
                .from(CERTIFICATE)
                .where(CERTIFICATE.REMOVED.isNull())
                .and(CERTIFICATE.ID.eq(defaultCertId))
                .fetchInto(Certificate.class);
        if (certs.isEmpty()) {
            return null;
        }
        return certs.get(0);
    }

    @Override
    public HealthcheckInstanceHostMap getHealthCheckInstanceUUID(String hostUUID, String instanceUUID) {
        MultiRecordMapper<HealthcheckInstanceHostMap> mapper = new MultiRecordMapper<HealthcheckInstanceHostMap>() {
            @Override
            protected HealthcheckInstanceHostMap map(List<Object> input) {
                if (input.get(0) != null) {
                    return (HealthcheckInstanceHostMap) input.get(0);
                }
                return null;
            }
        };

        HealthcheckInstanceHostMapTable hostMap = mapper.add(HEALTHCHECK_INSTANCE_HOST_MAP);
        InstanceTable instance = mapper.add(INSTANCE, INSTANCE.UUID, INSTANCE.ID);
        HostTable host = mapper.add(HOST, HOST.UUID, HOST.ID);
        List<HealthcheckInstanceHostMap> maps = create()
                .select(mapper.fields())
                .from(hostMap)
                .join(instance)
                .on(instance.ID.eq(hostMap.INSTANCE_ID))
                .join(host)
                .on(host.ID.eq(hostMap.HOST_ID))
                .where(host.UUID.eq(hostUUID))
                .and(instance.UUID.eq(instanceUUID))
                .and(hostMap.REMOVED.isNull())
                .fetch().map(mapper);

        if (maps.size() == 0) {
            return null;
        }
        return maps.get(0);
    }


    @Override
    public Map<Long, List<HealthcheckState>> getHealthcheckStatesForInstances(List<Long> ids,
            final IdFormatter idFormatter) {
        final Map<Long, List<HealthcheckState>> result = new HashMap<>();
        create().select(HEALTHCHECK_INSTANCE_HOST_MAP.INSTANCE_ID, HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID,
                HEALTHCHECK_INSTANCE_HOST_MAP.HEALTH_STATE)
                .from(HEALTHCHECK_INSTANCE_HOST_MAP)
                .join(HOST)
                .on(HOST.ID.eq(HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID))
                .where(HEALTHCHECK_INSTANCE_HOST_MAP.REMOVED.isNull()
                        .and(HOST.REMOVED.isNull())
                        .and(HEALTHCHECK_INSTANCE_HOST_MAP.INSTANCE_ID.in(ids)))
                .fetchInto(new RecordHandler<Record3<Long, Long, String>>() {
                    @Override
                    public void next(Record3<Long, Long, String> record) {
                        Long instanceId = record.getValue(HEALTHCHECK_INSTANCE_HOST_MAP.INSTANCE_ID);
                        Long hostId = record.getValue(HEALTHCHECK_INSTANCE_HOST_MAP.HOST_ID);
                        String healthState = record.getValue(HEALTHCHECK_INSTANCE_HOST_MAP.HEALTH_STATE);
                        List<HealthcheckState> list = result.get(instanceId);
                        if (list == null) {
                            list = new ArrayList<>();
                            result.put(instanceId, list);
                        }
                        HealthcheckState state = new HealthcheckState(idFormatter.formatId("host", hostId).toString(), healthState);
                        list.add(state);
                    }
                });

        return result;
    }

    @Override
    public List<? extends HealthcheckInstance> findBadHealthcheckInstance(int limit) {
        return create()
                .select(HEALTHCHECK_INSTANCE.fields())
                .from(HEALTHCHECK_INSTANCE)
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(HEALTHCHECK_INSTANCE.INSTANCE_ID))
                .where(INSTANCE.STATE.eq(CommonStatesConstants.PURGED)
                        .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())
                        .and(HEALTHCHECK_INSTANCE.STATE.notIn(CommonStatesConstants.DEACTIVATING,
                                CommonStatesConstants.REMOVING)))
                .limit(limit)
                .fetchInto(HealthcheckInstanceRecord.class);
    }

    @Override
    public List<? extends Service> getSkipServices(long accountId) {
        return create()
                .select(SERVICE.fields())
                .from(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId)
                        .and(SERVICE.REMOVED.isNull())
                        .and(SERVICE.SKIP.isTrue()))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public Map<String, DeploymentUnit> getDeploymentUnits(Service service) {
        List<? extends DeploymentUnit> units = create()
                .select(DEPLOYMENT_UNIT.fields())
                .from(DEPLOYMENT_UNIT)
                .where(DEPLOYMENT_UNIT.SERVICE_ID.eq(service.getId())
                        .and(DEPLOYMENT_UNIT.REMOVED.isNull())
                        .and(DEPLOYMENT_UNIT.STATE.notIn(CommonStatesConstants.REMOVING)))
                .fetchInto(DeploymentUnitRecord.class);

        Map<String, DeploymentUnit> uuidToUnit = new HashMap<>();
        for (DeploymentUnit unit : units) {
            uuidToUnit.put(unit.getUuid(), unit);
        }
        return uuidToUnit;
    }

    @Override
    public List<? extends Service> getServicesOnHost(long hostId) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .join(INSTANCE_HOST_MAP)
                .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .where(INSTANCE_HOST_MAP.HOST_ID.eq(hostId))
                .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                .and(SERVICE.REMOVED.isNull())
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstancesWithHealtcheckEnabled(long accountId) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .join(HEALTHCHECK_INSTANCE)
                .on(HEALTHCHECK_INSTANCE.INSTANCE_ID.eq(INSTANCE.ID))
                .and(HEALTHCHECK_INSTANCE.REMOVED.isNull())
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STATE.in(InstanceConstants.STATE_STARTING, InstanceConstants.STATE_RUNNING)
                        .and(INSTANCE.ACCOUNT_ID.eq(accountId)))
                .fetchInto(InstanceRecord.class);
    }

    private static class ServiceInstance {
        Instance instance;
        Long serviceId;

        public ServiceInstance(Instance instance, Long serviceId) {
            super();
            this.instance = instance;
            this.serviceId = serviceId;
        }

        public Instance getInstance() {
            return instance;
        }

        public Long getServiceId() {
            return serviceId;
        }
    }

    @Override
    public Map<Long, List<Instance>> getServiceInstancesWithNoDeploymentUnit() {
        final Map<Long, List<Instance>> serviceToInstances = new HashMap<>();

        MultiRecordMapper<ServiceInstance> mapper = new MultiRecordMapper<ServiceInstance>() {
            @Override
            protected ServiceInstance map(List<Object> input) {
                return new ServiceInstance((Instance) input.get(0),
                        ((ServiceExposeMap) input.get(1)).getServiceId());
            }
        };

        InstanceTable instance = mapper.add(INSTANCE);
        ServiceExposeMapTable exposeMap = mapper.add(SERVICE_EXPOSE_MAP);

        // fetch service instances having null du
        List<ServiceInstance> serviceInstances = create()
                .select(mapper.fields())
                .from(instance)
                .join(exposeMap)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID))
                .where(exposeMap.REMOVED.isNull())
                .and(instance.REMOVED.isNull())
                .and(instance.DEPLOYMENT_UNIT_UUID.isNull())
                .fetch().map(mapper);

        serviceInstances.addAll(create().select(mapper.fields())
                .from(instance)
                .join(exposeMap)
                .on(exposeMap.INSTANCE_ID.eq(instance.ID))
                .leftOuterJoin(DEPLOYMENT_UNIT)
                .on(DEPLOYMENT_UNIT.UUID.eq(instance.DEPLOYMENT_UNIT_UUID))
                .where(exposeMap.REMOVED.isNull())
                .and(instance.REMOVED.isNull())
                .and(instance.DEPLOYMENT_UNIT_UUID.isNotNull())
                .and(DEPLOYMENT_UNIT.UUID.isNull())
                .fetch().map(mapper));

        for (ServiceInstance si : serviceInstances) {
            List<Instance> instances = new ArrayList<>();
            Long svcId = si.getServiceId();
            if (serviceToInstances.containsKey(svcId)) {
                instances = serviceToInstances.get(svcId);
            }
            instances.add(si.getInstance());
            serviceToInstances.put(svcId, instances);
        }

        return serviceToInstances;
    }

    @Override
    public List<? extends DeploymentUnit> getUnitsOnHost(Host host, boolean transitioningOnly) {
        List<DeploymentUnit> toReturn = new ArrayList<>();
        Set<Long> processed = new HashSet<>();
        Condition condition = null;
        if (transitioningOnly) {
            List<String> transitioningStates = Arrays.asList(InstanceConstants.STATE_CREATING,
                    InstanceConstants.STATE_STOPPING, InstanceConstants.STATE_STARTING,
                    InstanceConstants.STATE_RESTARTING);
            condition = INSTANCE.ACCOUNT_ID.eq(host.getAccountId()).and(INSTANCE.STATE.in(transitioningStates));
        } else {
            condition = INSTANCE.ACCOUNT_ID.eq(host.getAccountId());
        }
        List<? extends DeploymentUnit> units =
                create().select(DEPLOYMENT_UNIT.fields())
                .from(DEPLOYMENT_UNIT)
                .join(INSTANCE)
                        .on(INSTANCE.DEPLOYMENT_UNIT_ID.eq(DEPLOYMENT_UNIT.ID))
                        .join(INSTANCE_HOST_MAP)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.HOST_ID.eq(host.getId()))
                .and(DEPLOYMENT_UNIT.REMOVED.isNull())
                        .and(condition)
                        .fetchInto(DeploymentUnitRecord.class);
        for (DeploymentUnit unit : units) {
            if (processed.contains(unit.getId())) {
                continue;
            }
            toReturn.add(unit);
        }
        return toReturn;
    }

    @Override
    public DeploymentUnit createDeploymentUnit(long accountId, Service service, Map<String, String> labels, Integer serviceIndex) {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put("uuid", io.cattle.platform.util.resource.UUID.randomUUID().toString());
        params.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX,
                serviceIndex);
        if (service != null) {
            params.put(ServiceConstants.FIELD_SERVICE_ID, service.getId());
        }
        if (labels != null) {
            params.put(InstanceConstants.FIELD_LABELS, labels);
        }
        return objectManager.create(DeploymentUnit.class, params);
    }

    @Override
    public DeploymentUnit joinDeploymentUnit(Instance instance) {
        if (isServiceManagedInstance(instance)) {
            return null;
        }
        List<Long> deps = InstanceConstants.getInstanceDependencies(instance);
        if (deps.isEmpty()) {
            return createDeploymentUnit(instance.getAccountId(), null, null,
                    null);
        }
        Instance depInstance = objectManager.findAny(Instance.class, INSTANCE.ID,
                deps.get(0));
        return objectManager.loadResource(DeploymentUnit.class, depInstance.getDeploymentUnitId());
    }

    @Override
    public Stack getOrCreateDefaultStack(final long accountId) {
        return lockManager.lock(new StackCreateLock(accountId), new LockCallback<Stack>() {
            @Override
            public Stack doWithLock() {
                List<? extends Stack> stacks = create()
                        .select(STACK.fields())
                        .from(STACK)
                        .where(STACK.ACCOUNT_ID.eq(accountId)
                                .and(STACK.REMOVED.isNull())
                                .and(STACK.NAME.equalIgnoreCase(ServiceConstants.DEFAULT_STACK_NAME)))
                        .fetchInto(StackRecord.class);
                if (stacks.size() > 0) {
                    return stacks.get(0);
                }

                return resourceDao.createAndSchedule(Stack.class,
                        STACK.ACCOUNT_ID, accountId,
                        STACK.NAME, ServiceConstants.DEFAULT_STACK_NAME,
                        STACK.HEALTH_STATE, HealthcheckConstants.HEALTH_STATE_HEALTHY);
            }
        });
    }

    @Override
    public InstanceRevision createRevision(Service service, Map<String, Object> primaryLaunchConfig,
            List<Map<String, Object>> secondaryLaunchConfigs, boolean isFirstRevision) {
        InstanceRevision revision = objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.SERVICE_ID,
                service.getId(),
                INSTANCE_REVISION.REMOVED, null);
        if ((revision != null && !isFirstRevision) || (revision == null && isFirstRevision)) {
            Map<String, Object> data = new HashMap<>();
            Map<String, Map<String, Object>> specs = new HashMap<>();
            specs.put(service.getName(), primaryLaunchConfig);
            if (secondaryLaunchConfigs != null && !secondaryLaunchConfigs.isEmpty()) {
                for (Map<String, Object> spec : secondaryLaunchConfigs) {
                    specs.put(spec.get("name").toString(), spec);
                }
            }
            data.put(InstanceConstants.FIELD_INSTANCE_SPECS, specs);
            data.put(ObjectMetaDataManager.NAME_FIELD, service.getName());
            data.put(ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId());
            data.put("serviceId", service.getId());
            revision = objectManager.create(InstanceRevision.class, data);
        }
        return revision;
    }

    @Override
    public void cleanupServiceRevisions(Service service) {
        List<InstanceRevision> revisions = objectManager.find(InstanceRevision.class, INSTANCE_REVISION.SERVICE_ID,
                service.getId(),
                INSTANCE_REVISION.REMOVED, null);
        for (InstanceRevision revision : revisions) {
            Map<String, Object> params = new HashMap<>();
            params.put(ObjectMetaDataManager.REMOVED_FIELD, new Date());
            params.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, new Date());
            params.put(ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.REMOVED);
            objectManager.setFields(revision, params);
        }
    }

    @Override
    public Pair<InstanceRevision, InstanceRevision> getCurrentAndPreviousRevisions(Service service) {
        InstanceRevision currentRevision = objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                service.getRevisionId());
        InstanceRevision previousRevision = objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                service.getPreviousRevisionId());
        return Pair.of(currentRevision, previousRevision);
    }

    @Override
    public InstanceRevision getCurrentRevision(Service service) {
        return objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                service.getRevisionId());
    }

    @Override
    public Pair<Map<String, Object>, List<Map<String, Object>>> getPrimaryAndSecondaryConfigFromRevision(
            InstanceRevision revision, Service service) {
        Map<String, Object> primary = new HashMap<>();
        List<Map<String, Object>> secondary = new ArrayList<>();
        Map<String, Map<String, Object>> specs = CollectionUtils.toMap(DataAccessor.field(
                revision, InstanceConstants.FIELD_INSTANCE_SPECS, Object.class));
        primary.putAll(specs.get(service.getName()));
        specs.remove(service.getName());
        secondary.addAll(specs.values());
        return Pair.of(primary, secondary);
    }

    @Override
    public InstanceRevision getRevision(Service service, long revisionId) {
        return objectManager.findAny(InstanceRevision.class, INSTANCE_REVISION.ID,
                revisionId, INSTANCE_REVISION.SERVICE_ID, service.getId());
    }

    @Override
    public List<Instance> getInstancesToGarbageCollect(Service service) {
        return create()
                        .select(INSTANCE.fields())
                        .from(INSTANCE)
                        .join(SERVICE_EXPOSE_MAP)
                        .on(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                        .where(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.REMOVING))
                        .and(SERVICE_EXPOSE_MAP.UPGRADE.eq(true))
                        .and(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(service.getId()))
                        .fetchInto(InstanceRecord.class);
    }
}
