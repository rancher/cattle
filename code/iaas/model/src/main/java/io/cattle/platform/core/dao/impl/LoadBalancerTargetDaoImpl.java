package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.LOAD_BALANCER_TARGET;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.model.tables.records.LoadBalancerTargetRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.List;

import javax.inject.Inject;

public class LoadBalancerTargetDaoImpl extends AbstractJooqDao implements LoadBalancerTargetDao {

    @Inject
    ObjectManager objectManager;

    @Override
    public LoadBalancerTarget getLbInstanceTarget(long lbId, long instanceId) {
        return objectManager.findOne(LoadBalancerTarget.class,
                LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lbId,
                LOAD_BALANCER_TARGET.INSTANCE_ID, instanceId);
    }

    @Override
    public LoadBalancerTarget getLbIpAddressTarget(long lbId, String ipAddress) {
        return objectManager.findOne(LoadBalancerTarget.class,
                LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lbId,
                LOAD_BALANCER_TARGET.IP_ADDRESS, ipAddress);
    }

    @Override
    public List<? extends LoadBalancerTarget> listByLbIdToRemove(long lbId) {
        return create()
                .selectFrom(LOAD_BALANCER_TARGET)
                .where(
                        LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)
                                .and(LOAD_BALANCER_TARGET.REMOVED.isNull().
                                        or(LOAD_BALANCER_TARGET.STATE.eq(CommonStatesConstants.REMOVING))))
                .fetchInto(LoadBalancerTargetRecord.class);
    }

    @Override
    public List<? extends LoadBalancerTarget> listByLbId(long lbId) {
        return create()
                .selectFrom(LOAD_BALANCER_TARGET)
                .where(
                        LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)
                                .and(LOAD_BALANCER_TARGET.REMOVED.isNull()))
                .fetchInto(LoadBalancerTargetRecord.class);
    }
}
