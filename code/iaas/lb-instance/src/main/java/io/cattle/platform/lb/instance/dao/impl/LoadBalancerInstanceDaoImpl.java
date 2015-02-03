package io.cattle.platform.lb.instance.dao.impl;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.lb.instance.dao.LoadBalancerInstanceDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class LoadBalancerInstanceDaoImpl extends AbstractJooqDao implements LoadBalancerInstanceDao {

    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Inject
    NetworkDao ntwkDao;

    @Inject
    AccountDao accountDao;

    @Override
    public List<Long> getLoadBalancerHosts(long lbId) {
        List<Long> hostIds = new ArrayList<Long>();
        for (LoadBalancerHostMap map : mapDao.findToRemove(LoadBalancerHostMap.class, LoadBalancer.class, lbId)) {
            hostIds.add(map.getHostId());
        }
        return hostIds;
    }

    @Override
    public Network getLoadBalancerInstanceNetwork(LoadBalancer loadBalancer) {
        List<? extends Network> accountNetworks = ntwkDao.getNetworksForAccount(loadBalancer.getAccountId(),
                NetworkConstants.KIND_HOSTONLY);
        if (accountNetworks.isEmpty()) {
            // pass system network if account doesn't own any
            List<? extends Network> systemNetworks = ntwkDao.getNetworksForAccount(accountDao.getSystemAccount()
                    .getId(),
                    NetworkConstants.KIND_HOSTONLY);
            if (systemNetworks.isEmpty()) {
                throw new RuntimeException(
                        "Unable to find a network to start a load balancer " + loadBalancer);
            }
            return systemNetworks.get(0);
        }

        return accountNetworks.get(0);
    }
}
