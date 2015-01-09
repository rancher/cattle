package io.cattle.platform.util.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedObject<T> implements Delayed {

    Long whenToRunMillis;
    T object;

    public DelayedObject(Long whenToRunMillis, T object) {
        super();
        this.whenToRunMillis = whenToRunMillis;
        this.object = object;
    }

    public T getObject() {
        return object;
    }

    @Override
    public int compareTo(Delayed o) {
        long thisVal = getDelay(TimeUnit.MILLISECONDS);
        long anotherVal = o.getDelay(TimeUnit.MILLISECONDS);

        if (thisVal < anotherVal) {
            return -1;
        }

        return (thisVal == anotherVal) ? 0 : 1;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(whenToRunMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof DelayedObject) {
            Object delayed = ((DelayedObject<?>) other).getObject();
            if (delayed == this.object) {
                return true;
            }
        }

        return super.equals(other);
    }
}