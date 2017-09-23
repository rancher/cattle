package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.CredentialRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static io.cattle.platform.core.model.tables.CredentialTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;


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
                        .and(INSTANCE.STATE.notIn(CommonStatesConstants.ERROR,
                                CommonStatesConstants.ERRORING,
                                CommonStatesConstants.REMOVING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public Instance getInstanceByUuidOrExternalId(Long clusterId, String uuid, String externalId) {
        Instance instance = null;
        Condition condition = INSTANCE.CLUSTER_ID.eq(clusterId);

        if(StringUtils.isNotEmpty(uuid)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.UUID.eq(uuid)))
                    .and(INSTANCE.REMOVED.isNull())
                    .fetchAny();
        }

        if (instance == null && StringUtils.isNotEmpty(externalId)) {
            instance = create()
                    .selectFrom(INSTANCE)
                    .where(condition
                    .and(INSTANCE.EXTERNAL_ID.eq(externalId)))
                    .and(INSTANCE.REMOVED.isNull())
                    .fetchAny();
        }

        return instance;
    }

    @Override
    public List<? extends Instance> getOtherDeploymentInstances(Instance instance) {
        if (instance.getDeploymentUnitId() == null) {
            return Collections.emptyList();
        }

        return create().select(INSTANCE.fields())
                .from(INSTANCE)
                .where(INSTANCE.DEPLOYMENT_UNIT_ID.eq(instance.getDeploymentUnitId())
                    .and(INSTANCE.STATE.ne(CommonStatesConstants.REMOVING))
                    .and(INSTANCE.DESIRED.isTrue())
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE.ID.ne(instance.getId())))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends Credential> getCredentials(Set<Long> credentialIds) {
        return create().select(CREDENTIAL.fields())
                .from(CREDENTIAL)
                .where(CREDENTIAL.ID.in(credentialIds))
                .fetchInto(CredentialRecord.class);
    }

}
