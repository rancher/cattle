package io.cattle.platform.activity;

public interface Entry extends AutoCloseable {

    void close();
    
    void exception(Throwable t);

}
