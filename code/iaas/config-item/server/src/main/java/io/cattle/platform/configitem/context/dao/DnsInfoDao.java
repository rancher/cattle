package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.core.model.Instance;

import java.util.List;

public interface DnsInfoDao {
    List<DnsEntryData> getInstanceLinksDnsData(Instance instance);

    List<DnsEntryData> getServiceDnsData(Instance instance, boolean isVIPProvider);
}
