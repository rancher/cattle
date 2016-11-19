package io.cattle.platform.systemstack.lock;

import io.cattle.platform.lock.definition.AbstractLockDefinition;

public class ProjectTemplateLoadLock extends AbstractLockDefinition {

    public ProjectTemplateLoadLock() {
        super("PROJECT.TEMPLATE.LOAD");
    }

}