package io.cattle.platform.activity;

import io.cattle.platform.core.model.Instance;

public interface ActivityLog {
    
    Entry start(Object actor, String type, String message);

    void info(String message, Object... args);

    void instance(Instance instance, String operation, String reason);

}
