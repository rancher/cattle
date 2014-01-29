package io.github.ibuildthecloud.dstack.process.common.handler;

import io.github.ibuildthecloud.dstack.engine.handler.AbstractProcessLogic;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;

import java.util.Map;

import javax.inject.Inject;

public abstract class AbstractObjectProcessLogic extends AbstractProcessLogic {

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    ObjectMetaDataManager objectMetaDataManager;

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    protected ExitReason activate(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.ACTIVATE, obj, data);
    }

    protected ExitReason deactivate(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.DEACTIVATE, obj, data);
    }

    protected ExitReason create(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, obj, data);
    }

    protected ExitReason remove(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.REMOVE, obj, data);
    }

    protected ExitReason restore(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.RESTORE, obj, data);
    }

    protected ExitReason deallocate(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.DEALLOCATE, obj, data);
    }

    protected ExitReason allocate(Object obj, Map<String,Object> data) {
        return getObjectProcessManager().executeStandardProcess(StandardProcess.ALLOCATE, obj, data);
    }

    protected ExitReason execute(String processName, Object resource, Map<String, Object> data) {
        ProcessInstance pi = getObjectProcessManager().createProcessInstance(processName, resource, data);
        return pi.execute();
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

    public ObjectMetaDataManager getObjectMetaDataManager() {
        return objectMetaDataManager;
    }

    @Inject
    public void setObjectMetaDataManager(ObjectMetaDataManager objectMetaDataManager) {
        this.objectMetaDataManager = objectMetaDataManager;
    }

}
