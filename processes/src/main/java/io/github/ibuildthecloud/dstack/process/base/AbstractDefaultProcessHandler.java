package io.github.ibuildthecloud.dstack.process.base;

import io.github.ibuildthecloud.dstack.engine.handler.AbstractProcessHandler;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.dstack.util.type.ScopeUtils;

import javax.inject.Inject;

public abstract class AbstractDefaultProcessHandler extends AbstractProcessHandler implements Priority {

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ScopeUtils.getScopeFromName(this) };
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectProcessManager getObjectProcessManager() {
        return objectProcessManager;
    }

    @Inject
    public void setObjectProcessManager(ObjectProcessManager objectProcessManager) {
        this.objectProcessManager = objectProcessManager;
    }

}
