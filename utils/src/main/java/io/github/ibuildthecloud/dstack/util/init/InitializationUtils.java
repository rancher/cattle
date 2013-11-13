package io.github.ibuildthecloud.dstack.util.init;

import io.github.ibuildthecloud.dstack.util.type.DelayInitialization;

public class InitializationUtils {

    public static void onInitialization(Object obj, Runnable runnable) {
        if ( obj instanceof DelayInitialization ) {
            ((DelayInitialization)obj).onInitialized(runnable);
        } else {
            runnable.run();
        }
    }

}
