package io.cattle.platform.trigger;

import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;

public class MetadataSyncTrigger implements Trigger {

    LoopManager loopManager;
    ClusterDao clusterDao;

    public MetadataSyncTrigger(LoopManager loopManager, ClusterDao clusterDao) {
        this.loopManager = loopManager;
        this.clusterDao = clusterDao;
    }

    @Override
    public void trigger(Long accountId, Long clusterId, Object resource, String source) {
        if (METADATA_SOURCE.equals(source) && clusterId != null) {
            Long clusterAccountId = clusterDao.getOwnerAcccountIdForCluster(clusterId);
            loopManager.kick(LoopFactory.METADATA_SYNC, Account.class, clusterAccountId, resource);
        }
    }

}
