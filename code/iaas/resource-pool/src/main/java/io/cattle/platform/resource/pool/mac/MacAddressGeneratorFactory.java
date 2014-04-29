package io.cattle.platform.resource.pool.mac;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.resource.pool.PooledResourceItemGenerator;
import io.cattle.platform.resource.pool.impl.AbstractTypeAndQualifierPooledItemGeneratorFactory;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import com.netflix.config.DynamicStringProperty;

public class MacAddressGeneratorFactory extends AbstractTypeAndQualifierPooledItemGeneratorFactory {

    private static final DynamicStringProperty MAC_UNASSIGNED_PREFIX = ArchaiusUtil.getString("mac.unassigned.prefix");

    public MacAddressGeneratorFactory() {
        super(Network.class, ResourcePoolConstants.MAC);
    }

    @Override
    protected PooledResourceItemGenerator createGenerator(Object pool, String qualifier) {
        String prefix = DataAccessor.field(pool, NetworkConstants.FIELD_MAC_PREFIX, String.class);
        if ( prefix == null ) {
            prefix = MAC_UNASSIGNED_PREFIX.get();
        }

        StringBuilder start = new StringBuilder(prefix);
        StringBuilder end = new StringBuilder(prefix);

        for ( ; start.length() < 16 ; ) {
            start.append(":00");
            end.append(":ff");
        }

        return new MacAddressGenerator(start.toString(), end.toString());
    }

}
