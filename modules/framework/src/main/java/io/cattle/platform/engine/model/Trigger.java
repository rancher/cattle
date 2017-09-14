package io.cattle.platform.engine.model;

public interface Trigger {

    String METADATA_SOURCE = "metadata";

    void trigger(Long accountId, Long clusterId, Object resource, String source);

}
