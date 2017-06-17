package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.GenericObjectTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceLinkTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceHostMapRecord;
import io.cattle.platform.core.model.tables.records.InstanceLinkRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.NicRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Named
public class InstanceDaoImpl extends AbstractJooqDao implements InstanceDao {
    @Inject
    ObjectManager objectManager;
    @Inject
    JsonMapper jsonMapper;
    @Inject
    NetworkDao networkDao;
    @Inject
    ObjectProcessManager objectProcessManager;
    @Inject
    TransactionDelegate transaction;

    LoadingCache<Long, Map<String, Object>> instanceData = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(new CacheLoader<Long, Map<String, Object>>() {
                @Override
                public Map<String, Object> load(Long key) throws Exception {
                    return lookupCacheInstanceData(key);
                }
            });

    @Override
    public List<? extends Instance> getNonRemovedInstanceOn(Long hostId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(hostId)
                            .and(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID)))
                .where(INSTANCE.REMOVED.isNull().and(
                        INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR, InstanceConstants.STATE_ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getInstanceByUuidOrExternalId(Long accountId, String uuid, String externalId) {
        Instance instance = null;
        Condition condition = INSTANCE.ACCOUNT_ID.eq(accountId);

        if(StringUtils.isNotEmpty(uuid)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.UUID.eq(uuid)))
                    .fetchAny();
        }

        if (instance == null && StringUtils.isNotEmpty(externalId)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.EXTERNAL_ID.eq(externalId)))
                    .fetchAny();
        }

        return instance;
    }

    @Override
    public List<? extends Service> findServicesFor(Instance instance) {
        return create().select(SERVICE.fields())
                .from(SERVICE)
                .join(SERVICE_EXPOSE_MAP)
                .on(SERVICE_EXPOSE_MAP.SERVICE_ID.eq(SERVICE.ID))
                .where(SERVICE_EXPOSE_MAP.INSTANCE_ID.eq(instance.getId()))
                .fetchInto(ServiceRecord.class);
    }

    @Override
    public List<? extends Instance> listNonRemovedNonStackInstances(Account account) {
        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.ACCOUNT_ID.eq(account.getId()))
                .and(INSTANCE.REMOVED.isNull())
                .and(INSTANCE.STACK_ID.isNull())
                .fetchInto(InstanceRecord.class);

    }

    @Override
    public List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(SERVICE_EXPOSE_MAP)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .where(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)
                    .and(INSTANCE.ACCOUNT_ID.eq(accountId))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.NAME.eq(serviceName)))
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Instance> findInstanceByServiceName(long accountId, String serviceName, String stackName) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(SERVICE_EXPOSE_MAP)
                .on(INSTANCE.ID.eq(SERVICE_EXPOSE_MAP.INSTANCE_ID))
            .join(SERVICE)
                .on(SERVICE.ID.eq(SERVICE_EXPOSE_MAP.SERVICE_ID))
            .join(STACK)
                .on(STACK.ID.eq(SERVICE.STACK_ID))
            .where(INSTANCE.STATE.eq(InstanceConstants.STATE_RUNNING)
                    .and(INSTANCE.ACCOUNT_ID.eq(accountId))
                    .and(SERVICE_EXPOSE_MAP.REMOVED.isNull())
                    .and(SERVICE.REMOVED.isNull())
                    .and(SERVICE.NAME.eq(serviceName))
                    .and(STACK.NAME.eq(stackName)))
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Host> findHosts(long accountId, long instanceId) {
        return create().select(HOST.fields())
                .from(INSTANCE)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(HOST)
                    .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(HOST.REMOVED.isNull()
                    .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                    .and(INSTANCE.ID.eq(instanceId)))
                .fetchInto(HostRecord.class);
    }

    protected Map<String, Object> lookupCacheInstanceData(long instanceId) {
        Instance instance = objectManager.loadResource(Instance.class, instanceId);
        if (instance == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> newData = new HashMap<>();
        newData.put(DataUtils.FIELDS, instance.getData().get(DataUtils.FIELDS));
        return newData;
    }

    @Override
    public Map<String, Object> getCacheInstanceData(long instanceId) {
        return instanceData.getUnchecked(instanceId);
    }

    @Override
    public void clearCacheInstanceData(long instanceId) {
        instanceData.invalidate(instanceId);
    }

    @Override
    public List<? extends Instance> findBadInstances(int count) {
        return create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .join(HOST)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
            .where(HOST.REMOVED.isNotNull().and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_STOPPING, CommonStatesConstants.REMOVING)))
            .limit(count)
            .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends InstanceHostMap> findBadInstanceHostMaps(int count) {
        return create().select(INSTANCE_HOST_MAP.fields())
            .from(INSTANCE_HOST_MAP)
            .join(INSTANCE)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .where(INSTANCE_HOST_MAP.REMOVED.isNull()
                    .and(INSTANCE.STATE.eq(AccountConstants.STATE_PURGED))
                    .and(INSTANCE_HOST_MAP.STATE.notIn(CommonStatesConstants.DEACTIVATING, CommonStatesConstants.REMOVING)))
            .limit(count)
            .fetchInto(InstanceHostMapRecord.class);
    }

    @Override
    public List<? extends Nic> findBadNics(int count) {
        return create().select(NIC.fields())
                .from(NIC)
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .where(NIC.REMOVED.isNull().and(INSTANCE.STATE.eq(AccountConstants.STATE_PURGED))
                        .and(NIC.STATE.notIn(CommonStatesConstants.DEACTIVATING, CommonStatesConstants.REMOVING)))
                .limit(count)
                .fetchInto(NicRecord.class);
    }

    @Override
    public List<? extends InstanceLink> findBadInstanceLinks(int count) {
        return create().select(INSTANCE_LINK.fields())
                .from(INSTANCE_LINK)
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(INSTANCE_LINK.TARGET_INSTANCE_ID))
                .where(INSTANCE.STATE.eq(AccountConstants.STATE_PURGED))
                .limit(count)
                .fetchInto(InstanceLinkRecord.class);
    }

    GenericObject getPullTask(long accountId, String image, Map<String, String> labels) {
        List<GenericObject> tasks = objectManager.find(GenericObject.class, GENERIC_OBJECT.ACCOUNT_ID, accountId,
                GENERIC_OBJECT.REMOVED, null);
        for (GenericObject task : tasks) {
            if (getPullTaskUuid(accountId, image, labels)
                    .equalsIgnoreCase(DataAccessor.fieldString(task, "pullTaskUuid"))) {
                return task;
            }
        }
        return null;
    }

    private String getPullTaskUuid(long accountId, String image, Map<String, String> labels) {
        if (labels == null) {
            labels = new HashMap<>();
        }
        try {
            return String.format("%d:%s:%s", accountId, image, jsonMapper.writeValueAsString(labels));
        } catch (IOException e) {
            return String.format("%d:%s:%s", accountId, image);
        }
    }

}
