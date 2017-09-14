package io.cattle.platform.metadata;

import java.util.List;

public interface MetadataManager {

    List<Long> getAgentProvider(String providedServiceLabel, long clusterId);

    List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long clusterId);

    Metadata getMetadataForCluster(long clusterId);

    Metadata getMetadataForAccount(long accountId);

}