package io.github.ibuildthecloud.gdapi.util;

import java.util.function.Supplier;

public interface TransactionDelegate {

    void doInTransaction(Runnable run);

    <T> T doInTransactionResult(Supplier<T> run);

    <T extends Throwable> void doInTransactionWithException(ExceptionRunnable<T> run) throws T;

    @FunctionalInterface
    interface ExceptionRunnable<T extends Throwable> {
        void run() throws T;
    }
}
