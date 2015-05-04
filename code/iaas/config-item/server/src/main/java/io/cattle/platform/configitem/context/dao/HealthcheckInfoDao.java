package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.HealthcheckData;
import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface HealthcheckInfoDao {
    public List<HealthcheckData> getHealthcheckEntries(Instance instance);
}
