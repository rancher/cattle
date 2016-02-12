package io.cattle.platform.iaas.api.filter.dynamic.schema;

import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class DynamicSchemaFilterLock extends AbstractBlockingLockDefintion {
    public DynamicSchemaFilterLock(String dynaimcSchemaName) {
        super("DYNAMICSCHEMA.LOCK." + dynaimcSchemaName);
    }
}
