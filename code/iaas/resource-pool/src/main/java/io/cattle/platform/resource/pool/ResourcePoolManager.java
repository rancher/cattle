package io.cattle.platform.resource.pool;

public interface ResourcePoolManager {

    PooledResource allocateResource(Object pool, Object owner);

}
