package io.cattle.platform.activity;

public interface ActivityLog {
    
    Entry start(Object actor, String type);

    void info(String message, Object... args);

}
