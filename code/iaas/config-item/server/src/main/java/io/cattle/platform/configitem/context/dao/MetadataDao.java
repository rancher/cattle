package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.MetadataEntry;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Offering;
import io.cattle.platform.core.model.Zone;

import java.util.List;

public interface MetadataDao {

    List<MetadataEntry> getMetaData(Instance agentInstance);

    Offering getInstanceOffering(Instance instance);

    Zone getZone(Instance instance);

}
