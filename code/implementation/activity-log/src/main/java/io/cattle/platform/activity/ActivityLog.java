package io.cattle.platform.activity;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;

public interface ActivityLog {

    public static final String INFO = "info";
    public static final String ERROR = "error";

    Entry start(Service service, String type, String message);

    void info(String message, Object... args);

    void instance(Instance instance, String operation, String reason, String level);

}
