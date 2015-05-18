package io.cattle.iaas.labels.service;

import io.cattle.platform.core.model.Label;

public interface LabelsService {
    Label getOrCreateLabel(long accountId, String key, String value, String type);

    void createContainerLabel(long accountId, long instanceId, String key, String value);
    void createHostLabel(long accountId, long hostId, String key, String value);

}
