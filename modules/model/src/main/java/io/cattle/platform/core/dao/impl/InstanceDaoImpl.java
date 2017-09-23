package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import org.jooq.Configuration;

import java.util.Collections;
import java.util.List;

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

}
