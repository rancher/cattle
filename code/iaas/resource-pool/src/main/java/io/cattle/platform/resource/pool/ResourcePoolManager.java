package io.cattle.platform.resource.pool;

import java.util.List;

public interface ResourcePoolManager {

    static final String DEFAULT_QUALIFIER = "default";
    static final String GLOBAL = "global";

    PooledResource allocateOneResource(Object pool, Object owner, PooledResourceOptions options);

    List<PooledResource> allocateResource(Object pool, Object owner, PooledResourceOptions options);

    void releaseResource(Object pool, Object owner);

    void releaseResource(Object pool, Object owner, PooledResourceOptions options);

    void transferResource(Object pool, Object owner, Object newOwner);

    void transferResource(Object pool, Object owner, Object newOwner, PooledResourceOptions options);

}
