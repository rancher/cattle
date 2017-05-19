package io.cattle.platform.inator.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ReconcileLock extends AbstractLockDefinition {

    public ReconcileLock(String type, Long id) {
        super("RECONCILE." + type + "." + id);
    }

}
