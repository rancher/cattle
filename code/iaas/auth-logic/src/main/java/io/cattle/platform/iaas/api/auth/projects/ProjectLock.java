package io.cattle.platform.iaas.api.auth.projects;

import io.cattle.platform.core.model.Account;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class ProjectLock extends AbstractBlockingLockDefintion {
    public ProjectLock(Account project) {
        super("PROJECT.LOCK." + project.getId());
    }
}
