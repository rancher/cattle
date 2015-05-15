package io.cattle.iaas.labels.service;

import io.cattle.platform.core.model.Label;

public interface LabelsService {
    Label getOrCreateLabel(Long accountId, String key, String value, String type);

    void createContainerLabel(Long accountId, Long instanceId, String key, String value);
    void createHostLabel(Long accountId, Long hostId, String key, String value);

}
