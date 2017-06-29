package io.cattle.platform.resource.pool;

import java.util.Iterator;

public interface PooledResourceItemGenerator extends Iterator<String> {
    public boolean isInPool(String resource);
}
