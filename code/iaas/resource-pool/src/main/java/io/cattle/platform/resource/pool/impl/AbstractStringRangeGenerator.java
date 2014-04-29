package io.cattle.platform.resource.pool.impl;

import io.cattle.platform.resource.pool.PooledResourceItemGenerator;

import java.util.Random;

public abstract class AbstractStringRangeGenerator implements PooledResourceItemGenerator {

    private static final Random RANDOM = new Random();

    long min, max, current, start, length, count = 0;
    boolean first = true;

    public AbstractStringRangeGenerator(String min, String max) {
        super();

        this.min = fromString(min);
        this.max = fromString(max);
        this.length = (this.max - this.min + 1);
        this.start = current = this.min + (long)(RANDOM.nextDouble() * length);
    }

    @Override
    public boolean hasNext() {
        return count < length;
    }

    @Override
    public String next() {
        String next = toString(current);

        if ( ++current > max ) {
            current = min;
        }

        count++;
        return next;
    }

    protected abstract long fromString(String value);

    protected abstract String toString(long value);

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}