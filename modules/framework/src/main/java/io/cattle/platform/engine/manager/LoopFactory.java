package io.cattle.platform.engine.manager;

import io.cattle.platform.engine.model.Loop;

public interface LoopFactory {

    String RECONCILE = "service-reconcile";
    String DU_RECONCILE = "deployment-unit-reconcile";
    String HEALTHCHECK_SCHEDULE = "healthcheck-schedule";
    String HEALTHSTATE_CALCULATE = "healthstate-calculate";
    String HEALTHCHECK_CLEANUP = "healthcheck-cleanup";
    String ENDPOINT_UPDATE = "endpoint-update";
    String SERVICE_MEMBERSHIP = "service-membership";
    String METADATA_CLIENT = "metadata-client";
    String METADATA_SYNC = "metadata-sync";
    String HOST_ENDPOINT_UPDATE = "host-endpoint-update";

    Loop buildLoop(String name, String type, Long id);

}
