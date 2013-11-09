package io.github.ibuildthecloud.dstack.util.concurrent;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class DelayedObject<T> implements Delayed {

    Long whenToRunMillis;
    T object;

    @Override
    public int compareTo(Delayed o) {
        long thisVal = getDelay(TimeUnit.MILLISECONDS);
        long anotherVal = o.getDelay(TimeUnit.MILLISECONDS);

        if (thisVal < anotherVal ) {
            return -1;
        }

        return if (thisVal == anotherVal ) ? 0 : 1 
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return TimeUnit.MILLISECONDS.;
    }

}
