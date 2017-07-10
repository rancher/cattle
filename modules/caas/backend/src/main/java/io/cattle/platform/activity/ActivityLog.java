package io.cattle.platform.activity;

public interface ActivityLog {

    String INFO = "info";
    String ERROR = "error";

    Entry start(Long serviceId, Long deploymentUnitId, String type, String message);

    void info(String message, Object... args);

    void error(String message, Object... args);

    void instance(Long instanceId, String operation, String reason, String level);

    void waiting();

}
