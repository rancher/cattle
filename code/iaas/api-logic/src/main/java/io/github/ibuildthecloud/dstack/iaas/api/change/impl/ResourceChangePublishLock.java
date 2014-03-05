package io.github.ibuildthecloud.dstack.iaas.api.change.impl;

import io.github.ibuildthecloud.dstack.lock.definition.AbstractLockDefinition;

public class ResourceChangePublishLock extends AbstractLockDefinition {

    public ResourceChangePublishLock() {
        super("RESOURCE.CHANGE.PUBLISH");
    }

}
