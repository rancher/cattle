package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.collect.Lists;

@Named
public class DnsInfoFactory extends AbstractAgentBaseContextFactory {
    @Inject
    DnsInfoDao dnsInfoDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        // 1. retrieve all instance links for the hosts
        List<DnsEntryData> dnsEntries = dnsInfoDao.getInstanceLinksHostDnsData(instance);
        // 2. retrieve all service links for the host
        dnsEntries.addAll(dnsInfoDao.getServiceHostDnsData(instance));
        // 3. retrieve self service links
        dnsEntries.addAll(dnsInfoDao.getSelfServiceLinks(instance));
        // 4. aggregate the links based on the source ip address
        Map<String, DnsEntryData> processedDnsEntries = new HashMap<>();
        for (DnsEntryData dnsEntry : dnsEntries) {
            Map<String, List<IpAddress>> resolve = new HashMap<>();
            DnsEntryData newData = null;
            if (processedDnsEntries.containsKey(dnsEntry.getSourceIpAddress().getAddress())) {
                newData = processedDnsEntries.get(dnsEntry.getSourceIpAddress().getAddress());
                resolve = newData.getResolve();
                for (String dnsName : dnsEntry.getResolve().keySet()) {
                    Set<IpAddress> ips = new HashSet<>();
                    if (resolve.containsKey(dnsName)) {
                        ips.addAll(resolve.get(dnsName));
                    }
                    ips.addAll(dnsEntry.getResolve().get(dnsName));
                    resolve.put(dnsName, Lists.newArrayList(ips));
                    newData.setResolve(resolve);
                }
            } else {
                newData = dnsEntry;
            }

            processedDnsEntries.put(dnsEntry.getSourceIpAddress().getAddress(), newData);
        }
        context.getData().put("dnsEntries", processedDnsEntries.values());
    }
}
