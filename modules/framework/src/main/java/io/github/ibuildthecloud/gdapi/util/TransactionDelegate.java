package io.github.ibuildthecloud.gdapi.util;

import java.util.function.Supplier;

public interface TransactionDelegate {

    public void doInTransaction(Runnable run);

    public <T> T doInTransactionResult(Supplier<T> run);

    public <T extends Throwable> void doInTransactionWithException(ExceptionRunnable<T> run) throws T;

    @FunctionalInterface
    public interface ExceptionRunnable<T extends Throwable> {
        void run() throws T;
    }
}
