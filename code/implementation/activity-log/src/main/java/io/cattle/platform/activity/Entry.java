package io.cattle.platform.activity;

public interface Entry extends AutoCloseable {

    @Override
    void close();

    void exception(Throwable t);

}
