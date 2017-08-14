package io.cattle.platform.trigger;


import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;

public class MetadataChangedTrigger implements Trigger {

    String[] loopNames;
    LoopManager loopManager;
    AccountDao accountDao;

    public MetadataChangedTrigger(LoopManager loopManager, AccountDao accountDao, String... loopNames) {
        this.loopManager = loopManager;
        this.loopNames = loopNames;
        this.accountDao = accountDao;
    }

    @Override
    public void trigger(Long accountId, Long clusterId, Object resource, String source) {
        if (!Trigger.METADATA_SOURCE.equals(source)) {
            return;
        }
        if (accountId == null) {
            accountId = accountDao.getAccountIdForCluster(clusterId);
        }

        if (accountId == null) {
            return;
        }

        for (String loop : loopNames) {
            loopManager.kick(loop, Account.class, accountId, null);
        }
    }

}
