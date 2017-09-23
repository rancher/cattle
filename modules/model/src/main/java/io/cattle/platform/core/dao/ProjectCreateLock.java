package io.cattle.platform.core.dao;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ProjectCreateLock extends AbstractBlockingLockDefintion {
    public ProjectCreateLock(Long clusterId, String name) {
        super("PROJECT.CREATE." + clusterId + "." + name.hashCode());
    }
}
