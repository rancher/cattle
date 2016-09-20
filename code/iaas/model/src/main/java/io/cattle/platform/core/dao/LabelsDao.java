package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.Label;

import java.util.List;

public interface LabelsDao {

    List<Label> getLabelsForHost(Long hostId);
}
