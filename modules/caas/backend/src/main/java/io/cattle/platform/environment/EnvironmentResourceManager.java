package io.cattle.platform.environment;

import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.service.Metadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface EnvironmentResourceManager {

    boolean hostHasContainerLabel(long accountId, long hostId, String labelKey, String labelValue);

    List<? extends Long> getActiveHosts(long accountId);

    List<? extends Long> getHosts(long accountId);

    // key -> [value,mapping.state]
    Map<String, String[]> getLabelsForHost(long accountId, long hostId);

    Stream<HostInfo> iterateHosts(QueryOptions options, List<String> orderedHostUUIDs);

    void changed(long accountId, List<?> objects);

    void changed(long accountId, Object objects);

    List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long accountId);

    List<Long> getAgentProvider(String providedServiceLabel, long accountId);

    List<Long> getAvailableHealthCheckHosts(long accountId);

    Metadata getMetadata(long accountId);

}