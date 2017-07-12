package io.cattle.platform.engine.model;

public interface Trigger {

    String METADATA_SOURCE = "metadata";

    void trigger(Long accountId, Object resource, String source);

}
