package io.cattle.platform.resource.pool.port;

import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;
import io.cattle.platform.resource.pool.impl.StringRangeGenerator;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

public class EnvironmentPortGeneratorFactory implements PooledResourceItemGeneratorFactory {

    @Override
    public PooledResourceItemGenerator getGenerator(Object pool, String qualifier) {
        if ((pool instanceof Account) && ResourcePoolConstants.ENVIRONMENT_PORT.equals(qualifier)) {
            Account env = (Account) pool;
            ServicesPortRange portRange = DataAccessor.field(env, AccountConstants.FIELD_PORT_RANGE, ServicesPortRange.class);
            if (portRange == null) {
                portRange = AccountConstants.getDefaultServicesPortRange();
            }
            return new StringRangeGenerator(portRange.getStartPort().toString(), portRange.getEndPort().toString());
        }
        return null;
    }
}
