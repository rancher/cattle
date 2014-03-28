package io.cattle.platform.object.impl;

public interface TransactionDelegate {

    public void doInTransaction(Runnable run);

}
