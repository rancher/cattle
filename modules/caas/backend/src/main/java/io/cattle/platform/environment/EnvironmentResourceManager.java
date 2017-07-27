package io.cattle.platform.environment;

import io.cattle.platform.core.cache.QueryOptions;
import io.cattle.platform.metadata.model.HostInfo;
import io.cattle.platform.metadata.service.Metadata;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface EnvironmentResourceManager {

    boolean hostHasContainerLabel(long accountId, String hostUuid, String labelKey, String labelValue);

    List<HostInfo> getActiveHosts(long accountId);

    List<HostInfo> getHosts(long accountId);

    Map<String, String> getLabelsForHost(long accountId, String hostUuid);

    Iterator<HostInfo> iterateHosts(QueryOptions options, List<String> orderedHostUUIDs);

    List<Long> getAgentProvider(String providedServiceLabel, long accountId);

    List<Long> getAgentProviderIgnoreHealth(String providedServiceLabel, long accountId);

    Metadata getMetadata(long accountId);

}