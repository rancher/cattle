package io.cattle.platform.object.impl;

public class TransactionDelegateImpl implements TransactionDelegate {

    @Override
    public void doInTransaction(Runnable run) {
        run.run();
    }

}
