package io.cattle.platform.resource.pool.port;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.PooledResourceItemGeneratorFactory;
import io.cattle.platform.resource.pool.impl.StringRangeGenerator;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import com.netflix.config.DynamicStringProperty;

public class LinkPortGeneratorFactory implements PooledResourceItemGeneratorFactory {

    private static final DynamicStringProperty LINK_PORT_START = ArchaiusUtil.getString("link.internal.port.start");
    private static final DynamicStringProperty LINK_PORT_END = ArchaiusUtil.getString("link.internal.port.end");

    @Override
    public PooledResourceItemGenerator getGenerator(Object pool, String qualifier) {
        if ((pool instanceof NetworkService) && ResourcePoolConstants.LINK_PORT.equals(qualifier)) {
            return new StringRangeGenerator(LINK_PORT_START.get(), LINK_PORT_END.get());
        }

        return null;
    }

}
