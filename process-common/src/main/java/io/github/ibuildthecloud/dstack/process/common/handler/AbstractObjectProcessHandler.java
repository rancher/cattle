package io.github.ibuildthecloud.dstack.process.common.handler;

import io.github.ibuildthecloud.dstack.engine.handler.AbstractProcessHandler;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;

import java.util.Map;

import javax.inject.Inject;

public abstract class AbstractObjectProcessHandler extends AbstractProcessHandler {

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;

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

    protected ExitReason activate(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.ACTIVATE, obj, data);
    }

    protected ExitReason create(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, obj, data);
    }

}
