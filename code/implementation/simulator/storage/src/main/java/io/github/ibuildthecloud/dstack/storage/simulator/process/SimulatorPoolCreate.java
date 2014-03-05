package io.github.ibuildthecloud.dstack.storage.simulator.process;

import javax.inject.Inject;

import static io.github.ibuildthecloud.dstack.core.model.tables.StoragePoolTable.*;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.process.common.handler.AbstractObjectProcessHandler;
import io.github.ibuildthecloud.dstack.storage.simulator.lock.SimExtPoolCreateLock;

public class SimulatorPoolCreate extends AbstractObjectProcessHandler {

    private static final String EXT_UUID = "sim-ext-pool";
    private static final String KIND = "sim";

    LockManager lockManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "storagepool.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        StoragePool pool = (StoragePool)state.getResource();

        if ( ! KIND.equals(pool.getKind()) || EXT_UUID.equals(pool.getUuid()) ) {
            return null;
        }

        StoragePool extPool = getExtPool();

        if ( extPool == null ) {
            lockManager.lock(new SimExtPoolCreateLock(), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    StoragePool extPool = getExtPool();

                    if ( extPool == null ) {
                        extPool = getObjectManager().create(StoragePool.class,
                                ObjectMetaDataManager.UUID_FIELD, EXT_UUID,
                                ObjectMetaDataManager.KIND_FIELD, KIND,
                                STORAGE_POOL.EXTERNAL, true);
                        getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, extPool, null);
                    }
                }
            });
        } else {
            getObjectProcessManager().executeStandardProcess(StandardProcess.CREATE, extPool, null);
        }

        return null;
    }

    protected StoragePool getExtPool() {
        return getObjectManager().findOne(StoragePool.class,
                ObjectMetaDataManager.UUID_FIELD, EXT_UUID);
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
