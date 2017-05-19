package io.cattle.platform.activity;

public interface ActivityLog {

    public static final String INFO = "info";
    public static final String ERROR = "error";

    Entry start(Long serviceId, Long deploymentUnitId, String type, String message);

    void info(String message, Object... args);

    void error(String message, Object... args);

    void instance(Long instanceId, String operation, String reason, String level);

    void waiting();

}
