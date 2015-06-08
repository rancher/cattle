package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.INSTANCE_LINK;
import static io.cattle.platform.core.model.tables.InstanceTable.INSTANCE;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.IP_ADDRESS_NIC_MAP;
import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceConsumeMap;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.tables.InstanceLinkTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressNicMapTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.ServiceConsumeMapTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DnsInfoDaoImpl extends AbstractJooqDao implements DnsInfoDao {

    @Override
    public List<DnsEntryData> getInstanceLinksHostDnsData(final Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<String>> resolve = new HashMap<>();
                List<String> ips = new ArrayList<>();
                ips.add(((IpAddress) input.get(1)).getAddress());
                // add all instance links
                resolve.put(((InstanceLink) input.get(0)).getLinkName(), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                data.setInstance((Instance)input.get(3));
                return data;
            }
        };

        InstanceLinkTable instanceLink = mapper.add(INSTANCE_LINK);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        InstanceTable targetInstance = INSTANCE.as("instance");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(instanceLink)
                .on(instanceLink.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(instanceLink.TARGET_INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(instanceLink.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING)))
                .fetch().map(mapper);
    }

    private String getDnsName(Object serviceConsumeMap, Object service) {
        String consumeMapName = ((ServiceConsumeMap) serviceConsumeMap).getName();
        if (consumeMapName != null && !consumeMapName.isEmpty()) {
            return consumeMapName;
        }
        return ((Service) service).getName();
    }

    @Override
    public List<DnsEntryData> getServiceHostDnsData(final Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<String>> resolve = new HashMap<>();
                List<String> ips = new ArrayList<>();
                ips.add(((IpAddress) input.get(1)).getAddress());
                String dnsName = getDnsName(input.get(0), input.get(4), input.get(5), false);
                resolve.put(dnsName, ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                data.setInstance((Instance)input.get(3));
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceConsumeMapTable serviceConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        InstanceTable targetInstance = INSTANCE.as("instance");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(serviceConsumeMap.CONSUMED_SERVICE_ID))
                .join(targetService)
                .on(serviceConsumeMap.CONSUMED_SERVICE_ID.eq(targetService.ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(targetServiceExposeMap.INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(serviceConsumeMap.REMOVED.isNull())
                        .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE))
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetService.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING)))
                .fetch().map(mapper);
    }

    @Override
    public List<DnsEntryData> getSelfServiceLinks(Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<String>> resolve = new HashMap<>();
                List<String> ips = new ArrayList<>();
                ips.add(((IpAddress) input.get(1)).getAddress());
                resolve.put(getDnsName(input.get(0), null, input.get(4), true), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                data.setInstance((Instance)input.get(3));
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        InstanceTable targetInstance = INSTANCE.as("instance");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(targetService)
                .on(targetService.ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(targetService.ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(targetServiceExposeMap.INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING)))
                .fetch().map(mapper);
    }


    @Override
    public List<DnsEntryData> getExternalServiceDnsData(final Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<String>> resolve = new HashMap<>();
                List<String> ips = new ArrayList<>();
                ips.add(((ServiceExposeMap) input.get(1)).getIpAddress());
                resolve.put(getDnsName(input.get(4), input.get(0)), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                data.setInstance((Instance) input.get(3));
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceConsumeMapTable serviceConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        NicTable clientNic = NIC.as("client_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(serviceConsumeMap.CONSUMED_SERVICE_ID))
                .join(targetService)
                .on(serviceConsumeMap.CONSUMED_SERVICE_ID.eq(targetService.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(serviceConsumeMap.REMOVED.isNull())
                        .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE))
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetService.REMOVED.isNull())
                        .and(targetServiceExposeMap.IP_ADDRESS.isNotNull()))
                .fetch().map(mapper);
    }


    @Override
    public List<DnsEntryData> getDnsServiceLinks(Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<String>> resolve = new HashMap<>();
                List<String> ips = new ArrayList<>();
                ips.add(((IpAddress) input.get(1)).getAddress());
                resolve.put(getDnsName(input.get(0), input.get(4), input.get(5), false), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                data.setInstance((Instance) input.get(3));
                return data;
            }
        };

        ServiceTable dnsService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        InstanceTable clientInstance = mapper.add(INSTANCE);
        ServiceConsumeMapTable dnsConsumeMap = mapper.add(SERVICE_CONSUME_MAP);
        ServiceExposeMapTable targetServiceExposeMap = mapper.add(SERVICE_EXPOSE_MAP);

        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        InstanceTable targetInstance = INSTANCE.as("instance");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");
        ServiceConsumeMapTable serviceConsumeMap = SERVICE_CONSUME_MAP.as("service_consume_map");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(clientServiceExposeMap)
                .on(clientServiceExposeMap.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(dnsConsumeMap)
                .on(dnsConsumeMap.SERVICE_ID.eq(clientServiceExposeMap.SERVICE_ID))
                .join(dnsService)
                .on(dnsConsumeMap.CONSUMED_SERVICE_ID.eq(dnsService.ID))
                .join(serviceConsumeMap)
                .on(serviceConsumeMap.SERVICE_ID.eq(dnsService.ID))
                .join(targetServiceExposeMap)
                .on(targetServiceExposeMap.SERVICE_ID.eq(serviceConsumeMap.CONSUMED_SERVICE_ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(targetServiceExposeMap.INSTANCE_ID))
                .join(targetInstance)
                .on(targetNic.INSTANCE_ID.eq(targetInstance.ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
                .join(clientInstance)
                .on(clientNic.INSTANCE_ID.eq(clientInstance.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.VNET_ID.isNotNull())
                        .and(NIC.REMOVED.isNull())
                        .and(targetIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(targetIpAddress.REMOVED.isNull())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(targetNicIpTable.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(targetNic.REMOVED.isNull())
                        .and(serviceConsumeMap.REMOVED.isNull())
                        .and(serviceConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE))
                        .and(dnsConsumeMap.REMOVED.isNull())
                        .and(dnsConsumeMap.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE))
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetInstance.STATE.in(InstanceConstants.STATE_RUNNING,
                                InstanceConstants.STATE_STARTING))
                        .and(dnsService.KIND.eq(ServiceDiscoveryConstants.KIND.DNSSERVICE.name()))
                        .and(dnsService.STATE.in(CommonStatesConstants.ACTIVATING,
                                CommonStatesConstants.ACTIVE)))
                .fetch().map(mapper);
    }

    protected String getDnsName(Object service, Object serviceConsumeMap, Object serviceExposeMap, boolean self) {

        String dnsPrefix = null;
        if (serviceExposeMap != null) {
            dnsPrefix = ((ServiceExposeMap) serviceExposeMap).getDnsPrefix();
        }

        String consumeMapName = null;
        if (serviceConsumeMap != null) {
            consumeMapName = ((ServiceConsumeMap) serviceConsumeMap).getName();
        }

        String primaryDnsName = (consumeMapName != null && !consumeMapName.isEmpty()) ? consumeMapName
                : ((Service) service).getName();
        String dnsName = primaryDnsName;
        if (self) {
            dnsName = dnsPrefix == null ? dnsName : dnsPrefix;
        } else {
            dnsName = dnsPrefix == null ? dnsName : dnsPrefix + "." + dnsName;
        }

        return dnsName;
    }

}
