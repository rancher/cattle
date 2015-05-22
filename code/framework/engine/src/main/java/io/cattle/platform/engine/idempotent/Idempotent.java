package io.cattle.platform.engine.idempotent;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.util.exception.ExceptionUtils;

import java.util.HashSet;
import java.util.Set;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.apache.commons.lang3.ObjectUtils;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;

public class Idempotent {

    private static final int LOOP_MAX = 1000;

    private static final DynamicBooleanProperty CHECKS = ArchaiusUtil.getBoolean("idempotent.checks");
    private static final DynamicIntProperty LOOP_COUNT = ArchaiusUtil.getInt("idempotent.retry.count");

    private static final ThreadLocal<Set<String>> STACK_TRACES = new ManagedThreadLocal<Set<String>>() {
        @Override
        protected Set<String> initialValue() {
            return new HashSet<>();
        }
    };
    private static final ThreadLocal<Boolean> DISABLED = new ManagedThreadLocal<Boolean>();
    private static final ThreadLocal<Long> LEVEL = new ManagedThreadLocal<Long>() {
        @Override
        protected Long initialValue() {
            return new Long(0);
        }
    };

    public static boolean enabled() {
        return CHECKS.get() && ! Boolean.TRUE.equals(DISABLED.get());
    }

    public static <T> T execute(IdempotentExecution<T> execution) {
        if (!Idempotent.enabled()) {
            return execution.execute();
        }

        long level = LEVEL.get();
        try {
            LEVEL.set(level + 1);
            T result = null;

            int max = level > 0 ? 1 : LOOP_COUNT.get();
            for (int i = 0; i < max; i++) {
                for (int j = 0 ; j < LOOP_MAX; j++) {
                    try {
                        T resultAgain = execution.execute();
                        if (i == 0) {
                            result = resultAgain;
                        }

                        if (Boolean.TRUE.equals(DISABLED.get())) {
                            return resultAgain;
                        }

                        if (!ObjectUtils.equals(result, resultAgain)) {
                            throw new OperationNotIdemponent("Result [" + result + "] does not match second result [" + resultAgain + "]");
                        }
                        i+=j;
                        break;
                    } catch (IdempotentRetryException e) {
                        if (j == LOOP_MAX - 1) {
                            throw new IllegalStateException("Executed [" + execution + "] " + LOOP_MAX + " times and never completed");
                        }
                    }
                }
            }

            return result;
        } finally {
            LEVEL.set(level);
        }
    }

    public static void tempDisable() {
        DISABLED.set(true);
    }

    public static <T> T change(IdempotentExecution<T> execution) {
        if (!Idempotent.enabled() || LEVEL.get() < 1) {
            return execution.execute();
        }

        Set<String> traces = STACK_TRACES.get();
        IdempotentRetryException e = new IdempotentRetryException();
        String trace = ExceptionUtils.toString(e);
        if (!traces.contains(trace)) {
            traces.add(trace);
            throw e;
        }

        return execution.execute();
    }

}
