package io.github.ibuildthecloud.dstack.object.impl;

public interface TransactionDelegate {

    public void doInTransaction(Runnable run);

}
