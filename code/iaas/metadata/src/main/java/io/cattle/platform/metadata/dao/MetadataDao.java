package io.cattle.platform.metadata.dao;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Zone;
import io.cattle.platform.metadata.data.MetadataEntry;

public interface MetadataDao {

    MetadataEntry getMetadataForInstance(Instance instance);

    Zone getZone(Instance instance);

}
