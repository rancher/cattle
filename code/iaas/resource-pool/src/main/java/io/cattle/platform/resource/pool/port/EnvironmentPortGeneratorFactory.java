package io.cattle.platform.resource.pool.port;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.util.PortRangeSpec;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;
import io.cattle.platform.resource.pool.impl.StringRangeGenerator;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class EnvironmentPortGeneratorFactory implements PooledResourceItemGeneratorFactory {

    /*
     * Read from config file to support upgrade scenario when default port range is not set on account
     */
    private static final DynamicStringProperty ENV_PORT_RANGE = ArchaiusUtil
            .getString("environment.services.port.range");

    @Override
    public PooledResourceItemGenerator getGenerator(Object pool, String qualifier) {
        if ((pool instanceof Account) && ResourcePoolConstants.ENVIRONMENT_PORT.equals(qualifier)) {
            Account env = (Account) pool;
            PortRangeSpec spec = null;
            String portRange = DataAccessor.fieldString(env, AccountConstants.FIELD_PORT_RANGE);
            if (StringUtils.isEmpty(portRange)) {
                spec = new PortRangeSpec(ENV_PORT_RANGE.get());
            } else {
                spec = new PortRangeSpec(portRange);
            }
            return new StringRangeGenerator(String.valueOf(spec.getStartPort()), String.valueOf(spec.getEndPort()));
        }
        return null;
    }
}
