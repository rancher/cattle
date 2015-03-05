package io.cattle.platform.lb.instance.dao.impl;

import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lb.instance.dao.LoadBalancerInstanceDao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class LoadBalancerInstanceDaoImpl extends AbstractJooqDao implements LoadBalancerInstanceDao {

    @Inject
    GenericMapDao mapDao;

    @Override
    public List<Long> getLoadBalancerHosts(long lbId) {
        List<Long> hostIds = new ArrayList<Long>();
        for (LoadBalancerHostMap map : mapDao.findToRemove(LoadBalancerHostMap.class, LoadBalancer.class, lbId)) {
            hostIds.add(map.getHostId());
        }
        return hostIds;
    }
}
