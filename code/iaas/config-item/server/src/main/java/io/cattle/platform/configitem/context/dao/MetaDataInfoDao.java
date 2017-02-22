package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.metadata.common.HostMetaData;
import io.cattle.platform.configitem.context.data.metadata.common.MetaHelperInfo;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MetaDataInfoDao {
    // data that is being streamed
    void fetchContainers(MetaHelperInfo helperInfo, OutputStream os);

    void fetchNetworks(MetaHelperInfo helperInfo, OutputStream os);

    void fetchContainerLinks(MetaHelperInfo helperInfo, OutputStream os);

    void fetchHosts(MetaHelperInfo helperInfo, OutputStream os);

    void fetchSelf(HostMetaData selfHost, String version, OutputStream os);

    void fetchServices(MetaHelperInfo helperInfo, OutputStream os);

    void fetchStacks(MetaHelperInfo helperInfo, OutputStream os);

    void fetchServiceLinks(MetaHelperInfo helperInfo, OutputStream os);

    void fetchServiceContainerLinks(MetaHelperInfo helperInfo, OutputStream os);
    // helper data

    Map<Long, List<HealthcheckInstanceHostMap>> getInstanceIdToHealthCheckers(Account account);

    Map<Long, HostMetaData> getHostIdToHostMetadata(Account account, Map<Long, Account> accounts,
            Set<Long> linkedServicesIds);
}
