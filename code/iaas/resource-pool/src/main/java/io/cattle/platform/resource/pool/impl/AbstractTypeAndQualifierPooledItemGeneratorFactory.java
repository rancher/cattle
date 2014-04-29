package io.cattle.platform.resource.pool.impl;

import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;

public abstract class AbstractTypeAndQualifierPooledItemGeneratorFactory implements PooledResourceItemGeneratorFactory {

    Class<?> typeClass;
    String qualifier;

    public AbstractTypeAndQualifierPooledItemGeneratorFactory(Class<?> typeClass, String qualifier) {
        super();
        this.typeClass = typeClass;
        this.qualifier = qualifier;
    }

    @Override
    public PooledResourceItemGenerator getGenerator(Object pool, String qualifier) {
        if ( pool == null || qualifier == null ) {
            return null;
        }

        if ( typeClass == null && this.qualifier.equals(qualifier) ) {
            return createGenerator(pool, qualifier);
        }

        if ( typeClass != null && typeClass.isAssignableFrom(pool.getClass()) && this.qualifier.equals(qualifier) ) {
            return createGenerator(pool, qualifier);
        }

        return null;
    }

    protected abstract PooledResourceItemGenerator createGenerator(Object pool, String qualifier);

}
