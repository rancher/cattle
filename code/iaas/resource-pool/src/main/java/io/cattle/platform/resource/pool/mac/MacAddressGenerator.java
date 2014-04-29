package io.cattle.platform.resource.pool.mac;

import org.apache.commons.lang3.StringUtils;

import io.cattle.platform.resource.pool.impl.AbstractStringRangeGenerator;

public class MacAddressGenerator extends AbstractStringRangeGenerator {

    int macLength;

    public MacAddressGenerator(String min, String max) {
        super(min, max);
        this.macLength = min.replace(":", "").length();
    }

    @Override
    protected long fromString(String value) {
        return Long.decode("0x" + value.replace(":", ""));
    }

    @Override
    protected String toString(long value) {
        String hex = StringUtils.leftPad(Long.toHexString(value), macLength, '0');
        StringBuilder buffer = new StringBuilder();
        for ( int i = 0 ; i < hex.length() ; i++ ) {
            if ( i > 0 && i % 2 == 0 ) {
                buffer.append(':');
            }
            buffer.append(hex.charAt(i));
        }

        return buffer.toString();
    }

}
