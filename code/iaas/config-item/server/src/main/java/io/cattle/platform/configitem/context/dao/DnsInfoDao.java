package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface DnsInfoDao {
    List<DnsEntryData> getInstanceLinksHostDnsData(Instance instance);

    List<DnsEntryData> getServiceDnsData(Instance instance, boolean isVIPProvider, boolean links);

    List<DnsEntryData> getSelfServiceData(Instance instance, boolean isVIPProvider);

    List<DnsEntryData> getExternalServiceDnsData(Instance instance, boolean links);

    List<DnsEntryData> getDnsServiceLinksData(Instance instance, boolean isVIPProvider, boolean links);
}
