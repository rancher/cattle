package io.cattle.platform.trigger;


import io.cattle.platform.core.model.Account;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Trigger;

public class MetadataChangedTrigger implements Trigger {

    String[] loopNames;
    LoopManager loopManager;

    public MetadataChangedTrigger(LoopManager loopManager, String... loopNames) {
        this.loopManager = loopManager;
        this.loopNames = loopNames;
    }

    @Override
    public void trigger(Long accountId, Object resource, String source) {
        if (!Trigger.METADATA_SOURCE.equals(source)) {
            return;
        }
        for (String loop : loopNames) {
            loopManager.kick(loop, Account.class, accountId, null);
        }
    }

}
