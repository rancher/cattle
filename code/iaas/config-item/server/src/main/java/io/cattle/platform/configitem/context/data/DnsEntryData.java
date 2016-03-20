package io.cattle.platform.configitem.context.data;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.docker.constants.DockerInstanceConstants;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryDnsUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

public class DnsEntryData {
    @JsonIgnore
    String sourceIpAddress;
    @JsonIgnore
    Map<String, Map<String, String>> resolveServicesAndContainers = new HashMap<>();
    @JsonIgnore
    Map<String, List<String>> aRecords = new HashMap<>();
    @JsonIgnore
    Map<String, String> cnameRecords = new HashMap<>();

    List<String> search = new ArrayList<>();
    List<String> recurse = new ArrayList<>();
    List<String> authoritative = new ArrayList<>();
    Map<String, Map<String, List<String>>> a = new HashMap<>();
    Map<String, Map<String, String>> cname = new HashMap<>();

    @JsonIgnore
    Instance instance;

    public DnsEntryData(String sourceIpAddress, Map<String, Map<String, String>> resolveServicesAndContainers,
            Map<String, String> resolveCname,
            Instance instance, List<String> searchDomains) {
        this.sourceIpAddress = sourceIpAddress;
        setResolveServicesAndContainers(resolveServicesAndContainers);
        this.setCnameRecords(resolveCname);
        setInstance(instance);
        if (searchDomains != null) {
            search = searchDomains;
        }
    }

    public String getSourceIpAddress() {
        return sourceIpAddress;
    }

    private void setResolveServicesAndContainers(Map<String, Map<String, String>> resolve) {
        if (resolve == null) {
            return;
        }
        this.resolveServicesAndContainers = resolve;
        Map<String, List<String>> aRecordsMerged = new HashMap<>();
        for (String serviceName : resolve.keySet()) {
            if (StringUtils.isEmpty(serviceName)) {
                continue;
            }
            aRecordsMerged.put(serviceName, Lists.newArrayList(resolve.get(serviceName).keySet()));
            for (String ipAddress : resolve.get(serviceName).keySet()) {
                String instanceName = resolve.get(serviceName).get(ipAddress);
                if (!StringUtils.isEmpty(instanceName)) {
                    List<String> ips = new ArrayList<>();
                    ips.add(ipAddress);
                    aRecordsMerged.put(instanceName.toLowerCase(), ips);
                }
            }
        }
        this.setaRecords(aRecordsMerged);
    }

    private void setCnameRecords(Map<String, String> cnameRecords) {
        if (cnameRecords == null) {
            return;
        }
        this.cnameRecords = cnameRecords;
        for (String dnsName : this.cnameRecords.keySet()) {
            if (StringUtils.isEmpty(dnsName)) {
                continue;
            }
            Map<String, String> records = new HashMap<>();
            records.put("answer", this.cnameRecords.get(dnsName));
            this.cname.put(dnsName, records);
        }
    }

    private void setaRecords(Map<String, List<String>> aRecords) {
        if (aRecords == null) {
            return;
        }
        this.aRecords = aRecords;
        for (String dnsName : this.aRecords.keySet()) {
            Map<String, List<String>> records = new HashMap<>();
            records.put("answer", this.aRecords.get(dnsName));
            this.a.put(dnsName, records);
        }
    }

    private Map<String, Map<String, String>> getResolveServicesAndContainers() {
        return resolveServicesAndContainers;
    }

    public List<String> getRecurse() {
        return recurse;
    }

    public void setRecurse(List<String> recurse) {
        this.recurse = recurse;
    }

    private void setInstance(Instance instance) {
        this.instance = instance;
        if (instance != null) {
            List<String> dns = DataAccessor.fieldStringList(instance, DockerInstanceConstants.FIELD_DNS);
            //remove itself from recurse
            dns.remove(ServiceDiscoveryDnsUtil.NETWORK_AGENT_IP);
            recurse.addAll(dns);
        }
        if ("default".equals(sourceIpAddress)) {
            recurse.add("PARENT_DNS");
            authoritative.add(NetworkConstants.INTERNAL_DNS_SEARCH_DOMAIN);
        }
    }

    public static Map<String, String> mergeCname(DnsEntryData first, DnsEntryData second) {
        Map<String, String> resolveCname = second.cnameRecords;
        for (String dnsName : first.cnameRecords.keySet()) {
            if (!resolveCname.containsKey(dnsName)) {
                resolveCname.putAll(first.cnameRecords);
            }
        }
        return resolveCname;
    }

    public static List<String> mergeSearchDomains(DnsEntryData first, DnsEntryData second) {

        List<String> searches = new ArrayList<>();
        if (second.search != null) {
            searches.addAll(second.search);
        }
        if (first.search != null) {
            for (String search : first.search) {
                if (!searches.contains(search)) {
                    searches.addAll(first.search);
                }
            }
        }

        return searches;
    }

    public static Map<String, Map<String, String>> mergeResolve(DnsEntryData first, DnsEntryData second) {
        Map<String, Map<String, String>> resolve = second.getResolveServicesAndContainers();
        for (String dnsName : first.getResolveServicesAndContainers().keySet()) {
            Map<String, String> ips = new HashMap<>();
            if (resolve.containsKey(dnsName)) {
                ips.putAll(resolve.get(dnsName));
            }
            ips.putAll(first.getResolveServicesAndContainers().get(dnsName));
            resolve.put(dnsName, ips);
        }
        return resolve;
    }

    public Instance getInstance() {
        return instance;
    }

    public Map<String, Map<String, List<String>>> getA() {
        return a;
    }

    public void setA(Map<String, Map<String, List<String>>> a) {
        this.a = a;
    }

    public Map<String, Map<String, String>> getCname() {
        return cname;
    }

    public void setCname(Map<String, Map<String, String>> cname) {
        this.cname = cname;
    }

    public List<String> getSearch() {
        return search;
    }

    public void setSearch(List<String> search) {
        this.search = search;
    }

    public List<String> getAuthoritative() {
        return authoritative;
    }

    public void setAuthoritative(List<String> authoritative) {
        this.authoritative = authoritative;
    }
}
