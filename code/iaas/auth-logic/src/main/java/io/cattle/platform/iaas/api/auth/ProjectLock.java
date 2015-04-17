package io.cattle.platform.iaas.api.auth;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;
import io.cattle.platform.lock.definition.LockDefinition;

public class ProjectLock extends AbstractBlockingLockDefintion {
    public ProjectLock(Account project) {
        super("PROJECT.LOCK." + project.getId());
    }
}
