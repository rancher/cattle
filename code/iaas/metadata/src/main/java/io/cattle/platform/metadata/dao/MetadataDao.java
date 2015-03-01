package io.cattle.platform.metadata.dao;

import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Offering;
import io.cattle.platform.core.model.Zone;
import io.cattle.platform.metadata.data.MetadataEntry;
import io.cattle.platform.metadata.data.MetadataRedirectData;

import java.util.List;

public interface MetadataDao {

    List<MetadataEntry> getMetadata(Instance agentInstance);

    MetadataEntry getMetadataForInstance(Instance instance);

    List<MetadataRedirectData> getMetadataRedirects(Agent agent);

    Offering getInstanceOffering(Instance instance);

    Zone getZone(Instance instance);

    List<? extends NetworkService> getMetadataServices(Instance instance);

}
