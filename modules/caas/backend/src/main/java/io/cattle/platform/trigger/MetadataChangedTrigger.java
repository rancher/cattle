package io.cattle.platform.trigger;


import io.cattle.platform.core.dao.ClusterDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;

public class MetadataChangedTrigger implements Trigger {

    String[] loopNames;
    LoopManager loopManager;
    ClusterDao clusterDao;

    public MetadataChangedTrigger(LoopManager loopManager, ClusterDao clusterDao, String... loopNames) {
        this.loopManager = loopManager;
        this.loopNames = loopNames;
        this.clusterDao = clusterDao;
    }

    @Override
    public void trigger(Long accountId, Long clusterId, Object resource, String source) {
        if (!Trigger.METADATA_SOURCE.equals(source)) {
            return;
        }
        if (accountId == null) {
            accountId = clusterDao.getOwnerAcccountIdForCluster(clusterId);
        }

        if (accountId == null) {
            return;
        }

        for (String loop : loopNames) {
            loopManager.kick(loop, Account.class, accountId, resource);
        }
    }

}
