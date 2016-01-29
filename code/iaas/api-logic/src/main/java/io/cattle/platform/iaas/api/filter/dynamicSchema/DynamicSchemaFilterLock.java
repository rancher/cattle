package io.cattle.platform.iaas.api.filter.dynamicSchema;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class DynamicSchemaFilterLock extends AbstractBlockingLockDefintion {
    public DynamicSchemaFilterLock(String dynaimcSchemaName) {
        super("DYNAMICSCHEMA.LOCK." + dynaimcSchemaName);
    }
}
