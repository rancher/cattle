package io.cattle.platform.api.change.impl;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ResourceChangePublishLock extends AbstractLockDefinition {

    public ResourceChangePublishLock() {
        super("RESOURCE.CHANGE.PUBLISH");
    }

}
