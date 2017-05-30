package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.CertificateTable.*;
import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.HealthcheckInstanceTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.RevisionTable.*;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceIndexTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.constants.AccountConstants;
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
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.model.tables.HealthcheckInstanceHostMapTable;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceIndexTable;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.core.model.tables.VolumeTemplateTable;
import io.cattle.platform.core.model.tables.records.DeploymentUnitRecord;
import io.cattle.platform.core.model.tables.records.HealthcheckInstanceRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceIndexRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.model.tables.records.StackRecord;
import io.cattle.platform.core.model.tables.records.VolumeTemplateRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.engine.handler.ProcessLogic;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.util.ArrayList;
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
    @Inject
    EventService eventService;

    @Override
    public Service getServiceByExternalId(Long accountId, String externalId) {
        return create().selectFrom(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .and(SERVICE.EXTERNAL_ID.eq(externalId))
                .fetchAny();
    }

    @Override
    public ServiceIndex createServiceIndex(Long serviceId, String launchConfigName, String serviceIndex) {
        List<String> configNames = new ArrayList<>();
        configNames.add(launchConfigName);
        List<? extends ServiceIndex> serviceIndexes = create()
                .select(SERVICE_INDEX.fields())
                .from(SERVICE_INDEX)
                .where(SERVICE_INDEX.SERVICE_INDEX_.eq(serviceIndex))
                .and(SERVICE_INDEX.SERVICE_ID.eq(serviceId))
                .and(SERVICE_INDEX.REMOVED.isNull())
                .and(SERVICE_INDEX.STATE.ne(CommonStatesConstants.REMOVING))
                .and(SERVICE_INDEX.LAUNCH_CONFIG_NAME.in(configNames))
                .fetchInto(ServiceIndexRecord.class);
        ServiceIndex index = null;
        if (serviceIndexes.isEmpty()) {
            index = resourceDao.createAndSchedule(ServiceIndex.class,
                    SERVICE_INDEX.SERVICE_ID, serviceId,
                    SERVICE_INDEX.LAUNCH_CONFIG_NAME, launchConfigName,
                    SERVICE_INDEX.SERVICE_INDEX_, serviceIndex);
        } else {
            index = serviceIndexes.get(0);
        }
        return index;
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
                .where(INSTANCE.STATE.eq(AccountConstants.STATE_PURGED)
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
                        .and(DEPLOYMENT_UNIT.REMOVED.isNull()))
                .fetchInto(DeploymentUnitRecord.class);

        Map<String, DeploymentUnit> uuidToUnit = new HashMap<>();
        for (DeploymentUnit unit : units) {
            uuidToUnit.put(unit.getUuid(), unit);
        }
        return uuidToUnit;
    }

    @Override
    public List<DeploymentUnit> getDeploymentUnitsForRevision(Service service, boolean currentRevision) {
        Condition condition = null;
        if (currentRevision) {
            condition = DEPLOYMENT_UNIT.REVISION_ID.eq(service.getRevisionId());
        } else {
            condition = DEPLOYMENT_UNIT.REVISION_ID.ne(service.getRevisionId());
        }
        return create()
                .select(DEPLOYMENT_UNIT.fields())
                .from(DEPLOYMENT_UNIT)
                .where(DEPLOYMENT_UNIT.SERVICE_ID.eq(service.getId())
                .and(condition)
                .and(DEPLOYMENT_UNIT.REMOVED.isNull())
                .and(DEPLOYMENT_UNIT.STATE.notIn(CommonStatesConstants.REMOVING)))
                .fetchInto(DeploymentUnitRecord.class);
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
    public List<? extends DeploymentUnit> getServiceDeploymentUnitsOnHost(Host host) {
        List<DeploymentUnit> toReturn = new ArrayList<>();
        Set<Long> processed = new HashSet<>();

        // 1. Get non empty units
        List<? extends DeploymentUnit> nonEmptyUnits =
                create().select(DEPLOYMENT_UNIT.fields())
                .from(DEPLOYMENT_UNIT)
                .join(INSTANCE)
                        .on(INSTANCE.DEPLOYMENT_UNIT_ID.eq(DEPLOYMENT_UNIT.ID))
                        .join(INSTANCE_HOST_MAP)
                        .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
                .and(INSTANCE.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.HOST_ID.eq(host.getId()))
                .and(DEPLOYMENT_UNIT.REMOVED.isNull())
                        .and(DEPLOYMENT_UNIT.SERVICE_ID.isNotNull())
                        .and(INSTANCE.ACCOUNT_ID.eq(host.getAccountId()))
                        .fetchInto(DeploymentUnitRecord.class);
        for (DeploymentUnit unit : nonEmptyUnits) {
            if (processed.contains(unit.getId())) {
                continue;
            }
            processed.add(unit.getId());
            toReturn.add(unit);
        }

        // 2. Get global units for the host
        // (they can exist w/o instances present)
        List<? extends DeploymentUnit> emptyUnits =
                create().select(DEPLOYMENT_UNIT.fields())
                        .from(DEPLOYMENT_UNIT)
                        .where(DEPLOYMENT_UNIT.REMOVED.isNull())
                        .and(DEPLOYMENT_UNIT.ACCOUNT_ID.eq(host.getAccountId()))
                        .and(DEPLOYMENT_UNIT.SERVICE_ID.isNotNull())
                        .and(DEPLOYMENT_UNIT.HOST_ID.eq(host.getId()))
                        .fetchInto(DeploymentUnitRecord.class);
        for (DeploymentUnit unit : emptyUnits) {
            if (processed.contains(unit.getId())) {
                continue;
            }
            processed.add(unit.getId());
            toReturn.add(unit);
        }

        return toReturn;
    }

    @Override
    public DeploymentUnit createDeploymentUnit(long accountId, Long serviceId, long stackId,
            Long hostId, String serviceIndex, Long revisionId, boolean active) {
        Map<String, Object> params = new HashMap<>();
        params.put("accountId", accountId);
        params.put(InstanceConstants.FIELD_SERVICE_INSTANCE_SERVICE_INDEX, serviceIndex);
        params.put(InstanceConstants.FIELD_SERVICE_ID, serviceId);
        params.put(InstanceConstants.FIELD_STACK_ID, stackId);
        params.put(InstanceConstants.FIELD_HOST_ID, hostId);
        params.put(InstanceConstants.FIELD_REVISION_ID, revisionId);
        if (hostId != null) {
            params.put(InstanceConstants.FIELD_LABELS, CollectionUtils.asMap(
                    ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID, hostId));
        }
        if (active) {
            params.put(ServiceConstants.PROCESS_DU_CREATE + ProcessLogic.CHAIN_PROCESS, ServiceConstants.PROCESS_DU_ACTIVATE);
            return resourceDao.createAndSchedule(DeploymentUnit.class, params);
        }
        return objectManager.create(DeploymentUnit.class, params);
    }

    @Override
    public Stack getOrCreateDefaultStack(final long accountId) {
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

    @Override
    public void cleanupRevisions(Service service) {
        List<Revision> revisions = objectManager.find(Revision.class, REVISION.SERVICE_ID,
                service.getId());
        for (Revision revision : revisions) {
            if (revision.getId().equals(service.getPreviousRevisionId())
                    || revision.getId().equals(service.getRevisionId())) {
                // only mark as removed; they will be delete as a part of
                // service record removal
                Map<String, Object> params = new HashMap<>();
                params.put(ObjectMetaDataManager.REMOVED_FIELD, new Date());
                params.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, new Date());
                params.put(ObjectMetaDataManager.STATE_FIELD, CommonStatesConstants.REMOVED);
                objectManager.setFields(revision, params);
            } else {
                List<DeploymentUnit> units = objectManager.find(DeploymentUnit.class, DEPLOYMENT_UNIT.SERVICE_ID,
                        service.getId(),
                        DEPLOYMENT_UNIT.REVISION_ID, revision.getId());
                for (DeploymentUnit unit : units) {
                    objectManager.setFields(unit, InstanceConstants.FIELD_REVISION_ID,
                            (Object) null);
                }
                objectManager.delete(revision);
            }

        }
    }

    @Override
    public List<InstanceData> getInstanceData(Long id, String uuid) {
        MultiRecordMapper<InstanceData> mapper = new MultiRecordMapper<InstanceData>() {
            @Override
            protected InstanceData map(List<Object> input) {
                InstanceData data = new InstanceData();
                data.instance = (Instance)input.get(0);
                data.serviceIndex = (ServiceIndex)input.get(1);
                data.serviceExposeMap = (ServiceExposeMap)input.get(2);
                return data;
            }
        };

        InstanceTable instance = mapper.add(INSTANCE);
        ServiceIndexTable serviceIndex = mapper.add(SERVICE_INDEX);
        ServiceExposeMapTable serviceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);

        return create()
            .select(mapper.fields())
            .from(instance)
            .leftOuterJoin(serviceIndex)
                .on(instance.SERVICE_INDEX_ID.eq(serviceIndex.ID))
            .leftOuterJoin(serviceExposeMap)
                .on(instance.SERVICE_ID.eq(serviceExposeMap.SERVICE_ID)
                        .and(instance.ID.eq(serviceExposeMap.INSTANCE_ID))
                        .and(serviceExposeMap.MANAGED.isTrue()))
            .where(instance.REMOVED.isNull()
                .and(instance.DEPLOYMENT_UNIT_ID.eq(id)
                        .or(instance.DEPLOYMENT_UNIT_UUID.eq(uuid))))
            .fetch().map(mapper);
    }

    @Override
    public Long getNextCreate(Long serviceId) {
        Long next = null;

        if (serviceId != null) {
            Long index = create().select(SERVICE.CREATE_INDEX)
                .from(SERVICE)
                .where(SERVICE.ID.eq(serviceId))
                .forUpdate()
                .fetchAny().value1();
            Condition cond = index == null ? SERVICE.CREATE_INDEX.isNull() : SERVICE.CREATE_INDEX.eq(index);
            next = index == null ? 1L : index+1;

            create().update(SERVICE)
                .set(SERVICE.CREATE_INDEX, next)
                .where(SERVICE.ID.eq(serviceId)
                        .and(cond))
                .execute();
        }

        return next;
    }

    @Override
    public Pair<Instance, ServiceExposeMap> createServiceInstance(final Map<String, Object> properties, Long serviceId, Long createIndex) {
        properties.put(InstanceConstants.FIELD_CREATE_INDEX, createIndex);
        Instance instance = objectManager.create(Instance.class, properties);

        Map<String, String> labels = CollectionUtils.toMap(properties.get(InstanceConstants.FIELD_LABELS));
        String dnsPrefix = labels.get(ServiceConstants.LABEL_SERVICE_LAUNCH_CONFIG);
        if (ServiceConstants.PRIMARY_LAUNCH_CONFIG_NAME.equalsIgnoreCase(dnsPrefix)) {
            dnsPrefix = null;
        }

        ServiceExposeMap map = null;

        if (serviceId != null ) {
            map = objectManager.create(ServiceExposeMap.class,
                SERVICE_EXPOSE_MAP.STATE, CommonStatesConstants.ACTIVE,
                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                SERVICE_EXPOSE_MAP.SERVICE_ID, serviceId,
                SERVICE_EXPOSE_MAP.ACCOUNT_ID, instance.getAccountId(),
                SERVICE_EXPOSE_MAP.DNS_PREFIX, dnsPrefix,
                SERVICE_EXPOSE_MAP.MANAGED, true);
        }

        return Pair.of(instance, map);
    }

    @Override
    public List<? extends VolumeTemplate> getVolumeTemplates(Long stackId) {
        return create()
            .select(VOLUME_TEMPLATE.fields())
            .from(VOLUME_TEMPLATE)
            .where(VOLUME_TEMPLATE.STACK_ID.eq(stackId)
                    .and(VOLUME_TEMPLATE.REMOVED.isNull()))
            .fetchInto(VolumeTemplateRecord.class);
    }

    @Override
    public List<VolumeData> getVolumeData(long deploymentUnitId) {
        MultiRecordMapper<VolumeData> mapper = new MultiRecordMapper<VolumeData>() {
            @Override
            protected VolumeData map(List<Object> input) {
                VolumeData data = new VolumeData();
                data.volume = (Volume) input.get(0);
                data.template = (VolumeTemplate) input.get(1);
                return data;
            }
        };

        VolumeTable volume = mapper.add(VOLUME);
        VolumeTemplateTable volumeTemplate = mapper.add(VOLUME_TEMPLATE);

        return create()
                .select(mapper.fields())
                .from(volume)
                .join(volumeTemplate)
                    .on(volume.VOLUME_TEMPLATE_ID.eq(volumeTemplate.ID))
                .where(volume.DEPLOYMENT_UNIT_ID.eq(deploymentUnitId))
                .fetch().map(mapper);
    }

}
