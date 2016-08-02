package io.cattle.platform.configitem.context.impl;

import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.configitem.server.model.ConfigItem;
import io.cattle.platform.configitem.server.model.impl.ArchiveContext;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DnsInfoFactory extends AbstractAgentBaseContextFactory {
    private static final Logger log = LoggerFactory.getLogger(DnsInfoFactory.class);

    @Inject
    DnsInfoDao dnsInfoDao;
    @Inject
    NetworkDao networkDao;
    @Inject
    JsonMapper jsonMapper;

    @Override
    protected void populateContext(Agent agent, Instance instance, ConfigItem item, ArchiveContext context) {
        if (instance == null) {
            return;
        }
        List<DnsEntryData> dnsEntries = new ArrayList<DnsEntryData>();
        // 1. retrieve all instance links for the hosts
        dnsEntries.addAll(dnsInfoDao.getInstanceLinksDnsData(instance));
        // 2. retrieve service dns records with links
        dnsEntries.addAll(dnsInfoDao.getServiceDnsData(instance, false));
        // 3. get data for "default" section
        dnsEntries.addAll(dnsInfoDao.getServiceDnsData(instance, true));

        // aggregate the links based on the source ip address
        Map<String, DnsEntryData> processedDnsEntries = new HashMap<>();
        for (DnsEntryData newEntry : dnsEntries) {
            if (StringUtils.isEmpty(newEntry.getSourceIpAddress())) {
                continue;
            }
            DnsEntryData toAdd = null;
            if (processedDnsEntries.containsKey(newEntry.getSourceIpAddress())) {
                DnsEntryData processedEntry = processedDnsEntries.get(newEntry.getSourceIpAddress());
                toAdd = new DnsEntryData(newEntry.getSourceIpAddress(), DnsEntryData.mergeResolve(newEntry,
                        processedEntry),
                        DnsEntryData.mergeCname(newEntry, processedEntry), newEntry.getInstance(),
                        DnsEntryData.mergeSearchDomains(newEntry, processedEntry));
            } else {
                toAdd = newEntry;
            }

            processedDnsEntries.put(newEntry.getSourceIpAddress(), toAdd);
        }
        
        try {
            context.getData().put("config", jsonMapper.writeValueAsString(processedDnsEntries));
        } catch (IOException e) {
            log.error("Failed to marshal dns config", e);
        }
    }



}
