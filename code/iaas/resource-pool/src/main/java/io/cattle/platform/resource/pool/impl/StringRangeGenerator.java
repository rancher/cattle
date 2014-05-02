package io.cattle.platform.resource.pool.impl;

public class StringRangeGenerator extends AbstractStringRangeGenerator {

    public StringRangeGenerator(String min, String max) {
        super(min, max);
    }

    @Override
    protected long fromString(String value) {
        return Long.parseLong(value);
    }

    @Override
    protected String toString(long value) {
        return Long.toString(value);
    }

}
