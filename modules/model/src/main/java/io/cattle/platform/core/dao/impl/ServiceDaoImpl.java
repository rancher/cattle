package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.core.model.tables.VolumeTable;
import io.cattle.platform.core.model.tables.VolumeTemplateTable;
import io.cattle.platform.core.model.tables.records.DeploymentUnitRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.core.model.tables.records.StackRecord;
import io.cattle.platform.core.model.tables.records.VolumeTemplateRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;
import org.jooq.Condition;
import org.jooq.Configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;
import static io.cattle.platform.core.model.tables.VolumeTable.*;
import static io.cattle.platform.core.model.tables.VolumeTemplateTable.*;

public class ServiceDaoImpl extends AbstractJooqDao implements ServiceDao {

    ObjectManager objectManager;
    LockManager lockManager;
    GenericResourceDao resourceDao;
    TransactionDelegate transaction;

    public ServiceDaoImpl(Configuration configuration, ObjectManager objectManager, LockManager lockManager,
            GenericResourceDao resourceDao, TransactionDelegate transaction) {
        super(configuration);
        this.objectManager = objectManager;
        this.lockManager = lockManager;
        this.resourceDao = resourceDao;
        this.transaction = transaction;
    }

    @Override
    public Service getServiceByExternalId(Long accountId, String externalId) {
        return create().selectFrom(SERVICE)
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .and(SERVICE.EXTERNAL_ID.eq(externalId))
                .fetchAny();
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
    public List<Long> getServiceDeploymentUnitsOnHost(Host host) {
        if (host == null) {
            return Collections.emptyList();
        }
        Long[] result = create().select(DEPLOYMENT_UNIT.ID)
                .from(DEPLOYMENT_UNIT)
                .where(DEPLOYMENT_UNIT.HOST_ID.eq(host.getId())
                    .and(DEPLOYMENT_UNIT.REMOVED.isNull()))
                .fetchArray(DEPLOYMENT_UNIT.ID);
        return Arrays.asList(result);
    }

    @Override
    public DeploymentUnit createDeploymentUnit(long accountId, long clusterId, Long serviceId, long stackId,
            Long hostId, String serviceIndex, Long revisionId, boolean active) {
        Map<Object, Object> params = new HashMap<>();
        params.put(ObjectMetaDataManager.ACCOUNT_FIELD, accountId);
        params.put(ObjectMetaDataManager.CLUSTER_FIELD, clusterId);
        params.put(InstanceConstants.FIELD_SERVICE_INDEX, serviceIndex);
        params.put(InstanceConstants.FIELD_SERVICE_ID, serviceId);
        params.put(InstanceConstants.FIELD_STACK_ID, stackId);
        params.put(INSTANCE.HOST_ID, hostId);
        params.put(InstanceConstants.FIELD_REVISION_ID, revisionId);
        if (hostId != null) {
            params.put(InstanceConstants.FIELD_LABELS, CollectionUtils.asMap(
                    ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID, hostId));
        }
        if (active) {
            params.put(ServiceConstants.PROCESS_DU_CREATE + ProcessHandler.CHAIN_PROCESS, ServiceConstants.PROCESS_DU_ACTIVATE);
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

                Account account = objectManager.loadResource(Account.class, accountId);
                return resourceDao.createAndSchedule(Stack.class,
                        STACK.ACCOUNT_ID, accountId,
                        STACK.CLUSTER_ID, account.getClusterId(),
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
                .where(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.STATE.ne(CommonStatesConstants.REMOVING))
                    .and(INSTANCE.DESIRED.isFalse())
                    .and(INSTANCE.SERVICE_ID.eq(service.getId()))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> getInstanceByDeploymentUnit(Long id) {
        return create()
            .select(INSTANCE.fields())
            .from(INSTANCE)
            .where(INSTANCE.REMOVED.isNull()
                .and(INSTANCE.DEPLOYMENT_UNIT_ID.eq(id)))
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public Long getNextCreate(Long serviceId) {
        return transaction.doInTransactionResult(() -> {
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
        });
    }

    @Override
    public Instance createServiceInstance(final Map<String, Object> properties, Long serviceId, Long createIndex) {
        properties.put(InstanceConstants.FIELD_CREATE_INDEX, createIndex);
        return objectManager.create(Instance.class, properties);
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

    @Override
    public Service findServiceByName(long accountId, String serviceName) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .where(SERVICE.REMOVED.isNull()
                        .and(SERVICE.ACCOUNT_ID.eq(accountId))
                        .and(SERVICE.NAME.eq(serviceName)))
                .fetchAnyInto(ServiceRecord.class);
    }

    @Override
    public Service findServiceByName(long accountId, String serviceName, String stackName) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(STACK)
                    .on(STACK.ID.eq(SERVICE.STACK_ID))
                .where(SERVICE.REMOVED.isNull()
                        .and(SERVICE.ACCOUNT_ID.eq(accountId))
                        .and(SERVICE.NAME.eq(serviceName))
                        .and(STACK.NAME.eq(stackName)))
                .fetchAnyInto(ServiceRecord.class);
    }

}
