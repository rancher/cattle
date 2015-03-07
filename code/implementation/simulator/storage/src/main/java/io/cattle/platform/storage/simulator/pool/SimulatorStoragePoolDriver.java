package io.cattle.platform.storage.simulator.pool;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.storage.pool.AbstractKindBasedStoragePoolDriver;
import io.cattle.platform.storage.pool.StoragePoolDriver;

public class SimulatorStoragePoolDriver extends AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    public static final String SIM_KIND = "sim";
    public static final String SIM_FORMAT = "sim";

    public SimulatorStoragePoolDriver() {
        super(SIM_KIND);
    }

    @Override
    protected boolean populateExtenalImageInternal(StoragePool pool, String uuid, Image image) {
        // image.setUuid(uuid);
        // image.setIsPublic(true);
        image.setName(uuid);
        image.setFormat(SIM_FORMAT);

        return true;
    }

}
