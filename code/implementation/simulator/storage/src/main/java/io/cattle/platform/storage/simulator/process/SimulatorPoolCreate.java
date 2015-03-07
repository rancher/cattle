package io.cattle.platform.storage.simulator.process;

import static io.cattle.platform.core.model.tables.StoragePoolTable.*;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.storage.simulator.lock.SimExtPoolCreateLock;

import javax.inject.Inject;

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
        final StoragePool pool = (StoragePool) state.getResource();

        if (!KIND.equals(pool.getKind()) || EXT_UUID.equals(pool.getUuid())) {
            return null;
        }

        StoragePool extPool = getExtPool();

        if (extPool == null) {
            lockManager.lock(new SimExtPoolCreateLock(), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    StoragePool extPool = getExtPool();

                    if (extPool == null) {
                        extPool = getObjectManager().create(StoragePool.class, ObjectMetaDataManager.UUID_FIELD, EXT_UUID, ObjectMetaDataManager.KIND_FIELD,
                                KIND, STORAGE_POOL.ACCOUNT_ID, pool.getAccountId(), STORAGE_POOL.EXTERNAL, true);
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
        return getObjectManager().findOne(StoragePool.class, ObjectMetaDataManager.UUID_FIELD, EXT_UUID);
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
