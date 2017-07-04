package io.cattle.platform.engine.manager;

import io.cattle.platform.engine.model.Loop;

public interface LoopFactory {

    public static final String RECONCILE = "service-reconcile";
    public static final String DU_RECONCILE = "deployment-unit-reconcile";
    public static final String HEALTHCHECK_SCHEDULE = "healthcheck-schedule";
    public static final String HEALTHSTATE_CALCULATE = "healthstate-calculate";
    public static final String HEALTHCHECK_CLEANUP = "healthcheck-cleanup";
    public static final String SYSTEM_STACK = "system-stack";
    public static final String ENDPOINT_UPDATE = "endpoint-update";
    public static final String SERVICE_MEMBERSHIP = "service-membership";

    Loop buildLoop(String name, String type, Long id);

}
