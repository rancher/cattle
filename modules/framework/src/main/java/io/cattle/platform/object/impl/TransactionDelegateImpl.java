package io.cattle.platform.object.impl;

import io.cattle.platform.util.exception.ExceptionUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.function.Supplier;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DefaultDSLContext;

public class TransactionDelegateImpl implements TransactionDelegate {

    Configuration configuration;

    public TransactionDelegateImpl(Configuration configuration) {
        super();
        this.configuration = configuration;
    }

    @Override
    public void doInTransaction(Runnable run) {
        try(DSLContext c = new DefaultDSLContext(configuration)) {
            c.transaction(() -> {
                run.run();
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Throwable> void doInTransactionWithException(ExceptionRunnable<T> run) throws T {
        Throwable[] t = new Throwable[1];
        try(DSLContext c = new DefaultDSLContext(configuration)) {
            c.transaction(() -> {
                try {
                    run.run();
                } catch (Throwable e) {
                    ExceptionUtils.rethrowRuntime(e);
                    t[0] = e;
                }
            });
        }
        if (t[0] != null) {
            throw (T)t[0];
        }
    }

    @Override
    public <T> T doInTransactionResult(Supplier<T> run) {
        try(DSLContext c = new DefaultDSLContext(configuration)) {
            return c.transactionResult(() -> {
                return run.get();
            });
        }
    }

}
