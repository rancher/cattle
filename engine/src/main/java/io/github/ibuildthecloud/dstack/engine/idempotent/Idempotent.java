package io.github.ibuildthecloud.dstack.engine.idempotent;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

import java.util.HashSet;
import java.util.Set;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.apache.commons.lang3.ObjectUtils;

import com.netflix.config.DynamicBooleanProperty;

public class Idempotent {

    private static final String DISABLE = "_disable";
    private static final int LOOP_MAX = 1000;

    private static final DynamicBooleanProperty runMultipleTimes = ArchaiusUtil.getBoolean("idempotent.reexecute");
    private static final DynamicBooleanProperty abortOnChange = ArchaiusUtil.getBoolean("idempotent.abort.on.change");

    private static final ThreadLocal<Set<String>> IDEMPOTENT = new ManagedThreadLocal<Set<String>>();

    public static <T> T execute(IdempotentExecution<T> execution) {
        Set<String> traces = null;

        try {
            if ( abortOnChange.get() ) {
                traces = new HashSet<String>();

                if ( IDEMPOTENT.get() == null ) {
                    IDEMPOTENT.set(traces);
                }
            }

            T result = null;

            outer:
            for ( int i = 0 ; i < 3 ; i++ ) {
                for ( int j = 0 ; j < LOOP_MAX ; j++ ) {
                    try {
                        T resultAgain = execution.execute();
                        if ( i == 0 ) {
                            result = resultAgain;
                        }
                        if ( isDisabled(traces) || ! runMultipleTimes.get() ) {
                            break outer;
                        }
                        if ( ! ObjectUtils.equals(result, resultAgain) ) {
                            throw new OperationNotIdemponent("Result [" + result + "] does not match second result [" + resultAgain + "]");
                        }
                        break;
                    } catch ( IdempotentRetry e ) {
                        if ( IDEMPOTENT.get() != traces )
                            throw e;
                        if ( j == LOOP_MAX - 1 ) {
                            throw new IllegalStateException("Executed [" + execution + "] " + LOOP_MAX + " times and never completed traces [" + traces + "]");
                        }
                    }
                }
            }

            return result;
        } finally {
            if ( traces != null && IDEMPOTENT.get() == traces ) {
                IDEMPOTENT.remove();
            }
        }
    }

    public static <T> T change(IdempotentExecution<T> execution) {
        Set<String> traces = IDEMPOTENT.get();
        if ( traces != null && ! isDisabled(traces) ) {
            IdempotentRetry e = new IdempotentRetry();
            String trace = ExceptionUtils.toString(e);
            if ( ! traces.contains(trace) ) {
                traces.add(trace);
                throw e;
            }
        }

        return execution.execute();
    }

    protected static boolean isDisabled(Set<String> traces) {
        return traces == null || traces.contains(DISABLE);
    }

    public static void tempDisable() {
        Set<String> traces = IDEMPOTENT.get();
        if ( traces != null ) {
            traces.add(DISABLE);
        }
    }

}
