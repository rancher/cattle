package io.cattle.platform.docker.storage.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.docker.storage.DockerStoragePoolDriver;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

import com.netflix.config.DynamicBooleanProperty;

public class DockerStoragePoolCreate extends AbstractObjectProcessLogic implements ProcessPreListener {

    private static final DynamicBooleanProperty CREATE_POOL = ArchaiusUtil.getBoolean("docker.auto.create.external.pool");

    DockerStoragePoolDriver poolDriver;

    @Override
    public String[] getProcessNames() {
        return new String[] { "storagepool.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        if ( ! CREATE_POOL.get() ) {
            return null;
        }

        StoragePool storagePool = (StoragePool)state.getResource();

        if ( poolDriver.isDockerPool(storagePool) ) {
            poolDriver.createDockerExternalPool();
        }

        return null;
    }

    public DockerStoragePoolDriver getPoolDriver() {
        return poolDriver;
    }

    @Inject
    public void setPoolDriver(DockerStoragePoolDriver poolDriver) {
        this.poolDriver = poolDriver;
    }

}
