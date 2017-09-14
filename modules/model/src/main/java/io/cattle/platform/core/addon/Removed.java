package io.cattle.platform.core.addon;

import io.cattle.platform.core.addon.metadata.MetadataObject;

public class Removed {

    MetadataObject obj;

    public Removed(MetadataObject obj) {
        this.obj = obj;
    }

    public MetadataObject getRemoved() {
        return obj;
    }

}
