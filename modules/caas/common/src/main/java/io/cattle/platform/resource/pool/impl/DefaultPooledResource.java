package io.cattle.platform.resource.pool.impl;

import io.cattle.platform.resource.pool.PooledResource;

public class DefaultPooledResource implements PooledResource {

    String name;

    public DefaultPooledResource(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

}
