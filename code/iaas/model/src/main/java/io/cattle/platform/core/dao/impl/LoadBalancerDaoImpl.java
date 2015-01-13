package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
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

}
