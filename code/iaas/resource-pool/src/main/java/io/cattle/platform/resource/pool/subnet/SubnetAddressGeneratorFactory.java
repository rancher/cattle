package io.cattle.platform.resource.pool.subnet;

import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.impl.AbstractTypeAndQualifierPooledItemGeneratorFactory;

public class SubnetAddressGeneratorFactory extends AbstractTypeAndQualifierPooledItemGeneratorFactory {

    public SubnetAddressGeneratorFactory() {
        super(Subnet.class, ResourcePoolManager.DEFAULT_QUALIFIER);
    }

    @Override
    protected PooledResourceItemGenerator createGenerator(Object pool, String qualifier) {
        Subnet subnet = (Subnet)pool;
        return new SubnetAddressGenerator(subnet.getStartAddress(), subnet.getEndAddress());
    }

}
