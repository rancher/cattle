package io.cattle.platform.resource.pool;

public interface PooledResourceItemGeneratorFactory {

    PooledResourceItemGenerator getGenerator(Object pool, String qualifier);

}
