package io.github.ibuildthecloud.dstack.docker.storage.process;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.docker.storage.DockerStoragePoolDriver;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.handler.ProcessPreListener;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.process.common.handler.AbstractObjectProcessLogic;

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
