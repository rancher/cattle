package io.github.ibuildthecloud.dstack.deferred.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.ibuildthecloud.dstack.eventing.EventService;
import io.github.ibuildthecloud.dstack.eventing.model.Event;

public class DeferredUtils {

    private static final Logger log = LoggerFactory.getLogger(DeferredUtils.class);

    private static final ManagedThreadLocal<List<Runnable>> TL = new ManagedThreadLocal<List<Runnable>>() {
        @Override
        protected List<Runnable> initialValue() {
            return new ArrayList<Runnable>();
        }
    };

    public static void deferEvent(final Event event, final EventService service) {
        if ( service != null && event != null ) {
            defer(new Runnable() {
                @Override
                public void run() {
                    service.publish(event);
                }
            });
        }
    }

    public static void defer(Runnable runnable) {
        TL.get().add(runnable);
    }

    public static void runDeferred() {
        for ( Runnable runnable : TL.get() ) {
            try {
                runnable.run();
            } catch ( Throwable t ) {
                log.error("Failed to run deferred action", t);
            }
        }
    }
}
