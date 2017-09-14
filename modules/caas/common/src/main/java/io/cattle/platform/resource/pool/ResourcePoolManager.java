package io.cattle.platform.resource.pool;

import java.util.List;

public interface ResourcePoolManager {

    String DEFAULT_QUALIFIER = "default";
    String GLOBAL = "global";

    PooledResource allocateOneResource(Object pool, Object owner, PooledResourceOptions options);

    List<PooledResource> allocateResource(Object pool, Object owner, PooledResourceOptions options);

    void releaseAllResources(Object owner);

    void releaseResource(Object pool, Object owner);

    void releaseResource(Object pool, Object owner, PooledResourceOptions options);

}
