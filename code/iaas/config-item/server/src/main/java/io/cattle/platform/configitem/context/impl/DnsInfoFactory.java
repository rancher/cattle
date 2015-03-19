package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DnsInfoFactory extends AbstractAgentBaseContextFactory {
    @Inject
    DnsInfoDao dnsInfoDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        // 1. retrieve all the links for the hosts
        List<? extends DnsEntryData> dnsEntries = dnsInfoDao.getHostDnsData(instance);
        // 2. aggregate the links based on the source ip address
        Map<String, DnsEntryData> processedDnsEntries = new HashMap<>();
        for (DnsEntryData dnsEntry : dnsEntries) {
            Map<String, List<? extends IpAddress>> resolve = new HashMap<>();
            DnsEntryData existingData = null;
            if (processedDnsEntries.containsKey(dnsEntry.getSourceIpAddress().getAddress())) {
                existingData = processedDnsEntries.get(dnsEntry.getSourceIpAddress().getAddress());
                resolve = existingData.getResolve();
                resolve.putAll(dnsEntry.getResolve());
            } else {
                existingData = dnsEntry;
            }

            processedDnsEntries.put(dnsEntry.getSourceIpAddress().getAddress(), existingData);
        }
        context.getData().put("dnsEntries", processedDnsEntries.values());
    }
}
