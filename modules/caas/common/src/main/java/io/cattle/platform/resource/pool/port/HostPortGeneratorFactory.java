package io.cattle.platform.resource.pool.port;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;
import io.cattle.platform.resource.pool.impl.StringRangeGenerator;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import com.netflix.config.DynamicStringProperty;

public class HostPortGeneratorFactory implements PooledResourceItemGeneratorFactory {

    private static final DynamicStringProperty HOST_PORT_START = ArchaiusUtil.getString("host.port.start");
    private static final DynamicStringProperty HOST_PORT_END = ArchaiusUtil.getString("host.port.end");

    @Override
    public PooledResourceItemGenerator getGenerator(Object pool, String qualifier) {
        if ((pool instanceof Host) && ResourcePoolConstants.HOST_PORT.equals(qualifier)) {
            return new StringRangeGenerator(HOST_PORT_START.get(), HOST_PORT_END.get());
        }

        return null;
    }

}
