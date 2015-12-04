package io.cattle.platform.configitem.context.impl;

import static io.cattle.platform.core.model.tables.InstanceHostMapTable.INSTANCE_HOST_MAP;
import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Nic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DnsInfoFactory extends AbstractAgentBaseContextFactory {
    @Inject
    DnsInfoDao dnsInfoDao;
    @Inject
    NetworkDao networkDao;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        boolean isVIPProviderConfigured = isVIPProviderConfigured(instance);
        List<DnsEntryData> dnsEntries = new ArrayList<DnsEntryData>();
        // 1. retrieve all instance links for the hosts
        dnsEntries.addAll(dnsInfoDao.getInstanceLinksDnsData(instance));
        // 2. retrieve service dns records
        dnsEntries.addAll(dnsInfoDao.getServiceDnsData(instance, isVIPProviderConfigured));

        // aggregate the links based on the source ip address
        Map<String, DnsEntryData> processedDnsEntries = new HashMap<>();
        for (DnsEntryData dnsEntry : dnsEntries) {
            DnsEntryData newData = null;
            if (processedDnsEntries.containsKey(dnsEntry.getSourceIpAddress())) {
                newData = processedDnsEntries.get(dnsEntry.getSourceIpAddress());
                populateARecords(dnsEntry, newData);
                populateCnameRecords(dnsEntry, newData);
            } else {
                newData = dnsEntry;
            }

            processedDnsEntries.put(dnsEntry.getSourceIpAddress(), newData);
        }
        context.getData().put("dnsEntries", processedDnsEntries.values());
    }

    protected void populateARecords(DnsEntryData dnsEntry, DnsEntryData newData) {
        Map<String, Map<String, String>> resolve = newData.getResolveServicesAndContainers();
        for (String dnsName : dnsEntry.getResolveServicesAndContainers().keySet()) {
            Map<String, String> ips = new HashMap<>();
            if (resolve.containsKey(dnsName)) {
                ips.putAll(resolve.get(dnsName));
            }
            ips.putAll(dnsEntry.getResolveServicesAndContainers().get(dnsName));
            resolve.put(dnsName, ips);
            newData.setResolveServicesAndContainers(resolve);
        }
    }

    protected void populateCnameRecords(DnsEntryData dnsEntry, DnsEntryData newData) {
        Map<String, String> resolveCname = newData.getResolveCname();
        for (String dnsName : dnsEntry.getResolveCname().keySet()) {
            if (!resolveCname.containsKey(dnsName)) {
                resolveCname.putAll(dnsEntry.getResolveCname());
            }
            newData.setResolveCname(resolveCname);
        }
    }

    protected boolean isVIPProviderConfigured(Instance instance) {
        Nic primaryNic = networkDao.getPrimaryNic(instance.getId());
        if (primaryNic == null) {
            return false;
        }
        List<? extends InstanceHostMap> hostMaps = objectManager.find(InstanceHostMap.class,
                INSTANCE_HOST_MAP.INSTANCE_ID,
                instance.getId());

        if (hostMaps.isEmpty()) {
            return false;
        }

        return networkDao.getServiceProviderInstanceOnHost(NetworkServiceConstants.KIND_VIP,
                hostMaps.get(0).getHostId()) != null;
    }
}
