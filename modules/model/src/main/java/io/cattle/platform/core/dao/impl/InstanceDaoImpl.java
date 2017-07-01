package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.GenericObjectTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.ServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.DataUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Configuration;

public class InstanceDaoImpl extends AbstractJooqDao implements InstanceDao {

    ObjectManager objectManager;
    JsonMapper jsonMapper;

    public InstanceDaoImpl(Configuration configuration, ObjectManager objectManager, JsonMapper jsonMapper) {
        super(configuration);
        this.objectManager = objectManager;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public List<? extends Instance> getNonRemovedInstanceOn(Long hostId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.REMOVED.isNull()
                        .and(INSTANCE.HOST_ID.eq(hostId))
                        .and(INSTANCE.STATE.notIn(InstanceConstants.STATE_ERROR,
                                InstanceConstants.STATE_ERRORING,
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

    protected Map<String, Object> lookupCacheInstanceData(long instanceId) {
        Instance instance = objectManager.loadResource(Instance.class, instanceId);
        if (instance == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> newData = new HashMap<>();
        newData.put(DataUtils.FIELDS, instance.getData().get(DataUtils.FIELDS));
        return newData;
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
