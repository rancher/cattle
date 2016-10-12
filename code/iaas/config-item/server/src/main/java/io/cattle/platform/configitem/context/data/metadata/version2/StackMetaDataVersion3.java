package io.cattle.platform.configitem.context.data.metadata.version2;

import io.cattle.platform.configitem.context.data.metadata.common.StackMetaData;

public class StackMetaDataVersion3 extends StackMetaDataVersion2 {

    public StackMetaDataVersion3(StackMetaData stackData) {
        super(stackData);
        if (this.name != null) {
            this.name = this.name.toLowerCase();
        }
    }
}

