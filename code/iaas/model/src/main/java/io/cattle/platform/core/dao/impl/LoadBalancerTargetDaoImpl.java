package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.LoadBalancerTargetTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
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
                LOAD_BALANCER_TARGET.INSTANCE_ID, instanceId,
                LOAD_BALANCER_TARGET.REMOVED, null);
    }

    @Override
    public LoadBalancerTarget getLbInstanceTargetToRemove(long lbId, long instanceId) {
        return create()
                .selectFrom(LOAD_BALANCER_TARGET)
                .where(
                        LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)
                                .and(LOAD_BALANCER_TARGET.INSTANCE_ID.eq(instanceId))
                                .and(LOAD_BALANCER_TARGET.REMOVED.isNull().
                                        or(LOAD_BALANCER_TARGET.STATE.eq(CommonStatesConstants.REMOVING))))
                .fetchOne();
    }

    @Override
    public LoadBalancerTarget getLbIpAddressTargetToRemove(long lbId, String ipAddress) {
        return create()
                .selectFrom(LOAD_BALANCER_TARGET)
                .where(
                        LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)
                                .and(LOAD_BALANCER_TARGET.IP_ADDRESS.eq(ipAddress))
                                .and(LOAD_BALANCER_TARGET.REMOVED.isNull().
                                        or(LOAD_BALANCER_TARGET.STATE.eq(CommonStatesConstants.REMOVING))))
                .fetchOne();
    }

    @Override
    public LoadBalancerTarget getLbIpAddressTarget(long lbId, String ipAddress) {
        return objectManager.findOne(LoadBalancerTarget.class,
                LOAD_BALANCER_TARGET.LOAD_BALANCER_ID, lbId,
                LOAD_BALANCER_TARGET.IP_ADDRESS, ipAddress,
                LOAD_BALANCER_TARGET.REMOVED, null);
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

    @Override
    public List<? extends Instance> getLoadBalancerActiveInstanceTargets(long lbId) {
        return create()
                .select(INSTANCE.fields())
                .from(INSTANCE)
                .join(LOAD_BALANCER_TARGET)
                .on(LOAD_BALANCER_TARGET.INSTANCE_ID.eq(INSTANCE.ID)
                        .and(LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId))
                        .and(LOAD_BALANCER_TARGET.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .where(INSTANCE.REMOVED.isNull().and(
                        INSTANCE.STATE.in(InstanceConstants.STATE_RUNNING, InstanceConstants.STATE_STARTING,
                                InstanceConstants.STATE_RESTARTING)))
                .fetchInto(InstanceRecord.class);
    }

    @Override
    public List<? extends LoadBalancerTarget> getLoadBalancerActiveIpTargets(long lbId) {
        return create()
                .select(LOAD_BALANCER_TARGET.fields())
                .from(LOAD_BALANCER_TARGET)
                .where(LOAD_BALANCER_TARGET.REMOVED.isNull()
                        .and(
                        LOAD_BALANCER_TARGET.STATE.in(CommonStatesConstants.ACTIVATING,
                                        CommonStatesConstants.ACTIVE))
                        .and(LOAD_BALANCER_TARGET.IP_ADDRESS.isNotNull())
                        .and(LOAD_BALANCER_TARGET.LOAD_BALANCER_ID.eq(lbId)))
                .fetchInto(LoadBalancerTargetRecord.class);
    }
}
