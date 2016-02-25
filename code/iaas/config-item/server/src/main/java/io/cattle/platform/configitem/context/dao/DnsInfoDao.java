package io.cattle.platform.configitem.context.dao;

import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;

import java.util.List;
import java.util.Map;

public interface DnsInfoDao {
    List<DnsEntryData> getInstanceLinksDnsData(Instance instance);

    List<DnsEntryData> getServiceDnsData(Instance instance, boolean forDefault);

    Map<Long, IpAddress> getInstanceWithHostNetworkingToIpMap(long accountId);
}
