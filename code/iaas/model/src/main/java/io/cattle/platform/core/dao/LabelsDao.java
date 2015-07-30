package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Label;

import java.util.List;

public interface LabelsDao {

    List<Label> getLabelsForInstance(Long instanceId);

    List<Label> getLabelsForHost(Long hostId);

    Label getLabelForInstance(long instanceId, String labelKey);

}
