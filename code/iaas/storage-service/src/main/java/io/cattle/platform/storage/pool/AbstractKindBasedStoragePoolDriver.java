package io.cattle.platform.storage.pool;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import io.cattle.platform.core.model.Image;
import io.cattle.platform.core.model.StoragePool;

public abstract class AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    String kind;
    String kindPrefix;

    public AbstractKindBasedStoragePoolDriver(String kind) {
        super();
        this.kind = kind;
        this.kindPrefix = kind + ":";
    }

    @Override
    public boolean supportsPool(StoragePool pool) {
        return kind.equals(pool.getKind());
    }

    @Override
    public boolean populateExtenalImage(StoragePool pool, String uuid, Image image) throws IOException {
        if ( ! uuid.startsWith(kindPrefix) )
            return false;

        return populateExtenalImageInternal(pool, uuid, image);
    }

    protected String stripKindPrefix(String str) {
        return StringUtils.removeStart(str, kindPrefix);
    }

    protected abstract boolean populateExtenalImageInternal(StoragePool pool, String uuid, Image image) throws IOException;


    public String getKind() {
        return kind;
    }


}
