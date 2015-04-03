package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerConfigListenerMapTable.LOAD_BALANCER_CONFIG_LISTENER_MAP;
import static io.cattle.platform.core.model.tables.LoadBalancerListenerTable.LOAD_BALANCER_LISTENER;
import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.core.model.tables.records.LoadBalancerListenerRecord;
import io.cattle.platform.core.model.tables.records.LoadBalancerRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class LoadBalancerDaoImpl extends AbstractJooqDao implements LoadBalancerDao {

    @Override
    public boolean updateLoadBalancer(long lbId, Long glbId, Long weight) {
        int i = create()
                .update(LOAD_BALANCER)
                .set(LOAD_BALANCER.GLOBAL_LOAD_BALANCER_ID, glbId)
                .set(LOAD_BALANCER.WEIGHT, weight)
                .where(LOAD_BALANCER.ID.eq(lbId))
                .execute();

        return i == 1;
    }

    @Override
    public List<? extends LoadBalancer> listByConfigId(long configId) {
        return create()
                .selectFrom(LOAD_BALANCER)
                .where(
                        LOAD_BALANCER.LOAD_BALANCER_CONFIG_ID.eq(configId)
                                .and(LOAD_BALANCER.REMOVED.isNull())).fetchInto(LoadBalancerRecord.class);
    }

    @Override
    public List<? extends LoadBalancerListener> listActiveListenersForConfig(long configId) {
        return create()
                .select(LOAD_BALANCER_LISTENER.fields())
                .from(LOAD_BALANCER_LISTENER)
                .join(LOAD_BALANCER_CONFIG_LISTENER_MAP)
                    .on(LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_LISTENER_ID.eq(LOAD_BALANCER_LISTENER.ID)
                        .and(LOAD_BALANCER_CONFIG_LISTENER_MAP.LOAD_BALANCER_CONFIG_ID.eq(configId))
                        .and(LOAD_BALANCER_CONFIG_LISTENER_MAP.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .where(LOAD_BALANCER_LISTENER.REMOVED.isNull())
                .fetchInto(LoadBalancerListenerRecord.class);
    }

    @Override
    public LoadBalancer getActiveLoadBalancerById(long lbId) {
        List<? extends LoadBalancer> lbs = create()
                .select(LOAD_BALANCER.fields())
                .from(LOAD_BALANCER)
                .where(LOAD_BALANCER.REMOVED.isNull()
                        .and(LOAD_BALANCER.ID.eq(lbId))
                        .and(
                        LOAD_BALANCER.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .fetchInto(LoadBalancerRecord.class);
        if (lbs.isEmpty()) {
            return null;
        }
        return lbs.get(0);
    }
}
