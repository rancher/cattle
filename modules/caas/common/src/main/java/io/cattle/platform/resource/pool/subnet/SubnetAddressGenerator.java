package io.cattle.platform.resource.pool.subnet;

import io.cattle.platform.resource.pool.impl.AbstractStringRangeGenerator;
import io.cattle.platform.util.net.NetUtils;

public class SubnetAddressGenerator extends AbstractStringRangeGenerator {

    public SubnetAddressGenerator(String min, String max) {
        super(min, max);
    }

    @Override
    protected long fromString(String value) {
        return NetUtils.ip2Long(value);
    }

    @Override
    protected String toString(long value) {
        return NetUtils.long2Ip(value);
    }

}
