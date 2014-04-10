package io.cattle.platform.resource.pool.subnet;

import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;

public class SubnetAddressGeneratorFactory implements PooledResourceItemGeneratorFactory {

    @Override
    public PooledResourceItemGenerator getGenerator(Object pool) {
        if ( pool instanceof Subnet ) {
            return new SubnetAddressGenerator((Subnet)pool);
        }

        return null;
    }

}
