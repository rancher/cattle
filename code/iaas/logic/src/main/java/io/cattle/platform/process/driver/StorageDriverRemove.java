package io.cattle.platform.process.driver;

import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class StorageDriverRemove extends AbstractDefaultProcessHandler {

    @Inject
    StoragePoolDao storagePoolDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        StorageDriver driver = (StorageDriver)state.getResource();
        for (StoragePool pool : storagePoolDao.findNonRemovedStoragePoolByDriver(driver.getId())) {
            objectProcessManager.scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE, StandardProcess.REMOVE,
                    pool, null);
        }
        return null;
    }

}