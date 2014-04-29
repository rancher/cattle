package io.cattle.platform.resource.pool;

public interface ResourcePoolManager {

    static final String DEFAULT_QUALIFIER = "default";
    static final String GLOBAL = "global";

    PooledResource allocateResource(Object pool, Object owner);

    PooledResource allocateResource(Object pool, String qualifier, Object owner);

    void releaseResource(Object pool, Object owner);

    void releaseResource(Object pool, String qualifier, Object owner);

}
