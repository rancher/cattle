package io.cattle.platform.storage.pool;

import io.cattle.platform.core.model.Image;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractKindBasedStoragePoolDriver implements StoragePoolDriver {

    String kind;
    String kindPrefix;

    public AbstractKindBasedStoragePoolDriver(String kind) {
        super();
        this.kind = kind;
        this.kindPrefix = kind + ":";
    }

    @Override
    public boolean populateImage(String uuid, Image image){
        if (!uuid.startsWith(kindPrefix))
            return false;

        return populateImageInternal(uuid, image);
    }

    protected String stripKindPrefix(String str) {
        return StringUtils.removeStart(str, kindPrefix);
    }

    protected abstract boolean populateImageInternal(String uuid, Image image);

    public String getKind() {
        return kind;
    }

}
