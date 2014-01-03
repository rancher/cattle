package io.github.ibuildthecloud.dstack.storage.simulator.pool;

import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.storage.pool.AbstractKindBasedStoragePoolDriver;
import io.github.ibuildthecloud.dstack.storage.pool.StoragePoolDriver;

public class SimulatorStoragePoolDriver extends AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    public static final String SIM_KIND = "sim";

    public SimulatorStoragePoolDriver() {
        super(SIM_KIND);
    }

    @Override
    protected boolean populateExtenalImageInternal(StoragePool pool, String uuid, Image image) {
        image.setUuid(uuid);
        image.setIsPublic(true);

        return true;
    }

}
