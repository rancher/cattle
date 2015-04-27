package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceLinkTable.INSTANCE_LINK;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.IP_ADDRESS_NIC_MAP;
import static io.cattle.platform.core.model.tables.IpAddressTable.IP_ADDRESS;
import static io.cattle.platform.core.model.tables.NicTable.NIC;
import static io.cattle.platform.core.model.tables.ServiceConsumeMapTable.SERVICE_CONSUME_MAP;
import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.SERVICE_EXPOSE_MAP;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import io.cattle.platform.configitem.context.dao.DnsInfoDao;
import io.cattle.platform.configitem.context.data.DnsEntryData;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.tables.InstanceLinkTable;
import io.cattle.platform.core.model.tables.IpAddressNicMapTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.ServiceConsumeMapTable;
import io.cattle.platform.core.model.tables.ServiceExposeMapTable;
import io.cattle.platform.core.model.tables.ServiceTable;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;

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
                Map<String, List<IpAddress>> resolve = new HashMap<>();
                List<IpAddress> ips = new ArrayList<>();
                ips.add((IpAddress) input.get(1));
                // add all instance links
                resolve.put(((InstanceLink) input.get(0)).getLinkName(), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                return data;
            }
        };

        InstanceLinkTable instanceLink = mapper.add(INSTANCE_LINK);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");

        return create()
                .select(mapper.fields())
                .from(NIC)
                .join(clientNic)
                .on(NIC.VNET_ID.eq(clientNic.VNET_ID))
                .join(instanceLink)
                .on(instanceLink.INSTANCE_ID.eq(clientNic.INSTANCE_ID))
                .join(targetNic)
                .on(targetNic.INSTANCE_ID.eq(instanceLink.TARGET_INSTANCE_ID))
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
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
                        .and(instanceLink.REMOVED.isNull()))
                .fetch().map(mapper);
    }

    @Override
    public List<DnsEntryData> getServiceHostDnsData(final Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<IpAddress>> resolve = new HashMap<>();
                List<IpAddress> ips = new ArrayList<>();
                ips.add((IpAddress) input.get(1));
                resolve.put(((Service) input.get(0)).getName(), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");
        ServiceExposeMapTable targetServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_target");
        ServiceConsumeMapTable serviceConsumeMap = SERVICE_CONSUME_MAP.as("service_consume_map");

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
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
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
                        .and(clientServiceExposeMap.REMOVED.isNull())
                        .and(targetServiceExposeMap.REMOVED.isNull())
                        .and(targetService.REMOVED.isNull()))
                .fetch().map(mapper);
    }

    @Override
    public List<DnsEntryData> getSelfServiceLinks(Instance instance) {
        MultiRecordMapper<DnsEntryData> mapper = new MultiRecordMapper<DnsEntryData>() {
            @Override
            protected DnsEntryData map(List<Object> input) {
                DnsEntryData data = new DnsEntryData();
                Map<String, List<IpAddress>> resolve = new HashMap<>();
                List<IpAddress> ips = new ArrayList<>();
                ips.add((IpAddress) input.get(1));
                resolve.put(((Service) input.get(0)).getName(), ips);
                data.setSourceIpAddress((IpAddress) input.get(2));
                data.setResolve(resolve);
                return data;
            }
        };

        ServiceTable targetService = mapper.add(SERVICE);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        NicTable clientNic = NIC.as("client_nic");
        NicTable targetNic = NIC.as("target_nic");
        IpAddressNicMapTable clientNicIpTable = IP_ADDRESS_NIC_MAP.as("client_nic_ip");
        IpAddressNicMapTable targetNicIpTable = IP_ADDRESS_NIC_MAP.as("target_nic_ip");
        ServiceExposeMapTable clientServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_client");
        ServiceExposeMapTable targetServiceExposeMap = SERVICE_EXPOSE_MAP.as("service_expose_map_target");

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
                .join(targetNicIpTable)
                .on(targetNicIpTable.NIC_ID.eq(targetNic.ID))
                .join(targetIpAddress)
                .on(targetNicIpTable.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(clientNicIpTable)
                .on(clientNicIpTable.NIC_ID.eq(clientNic.ID))
                .join(clientIpAddress)
                .on(clientNicIpTable.IP_ADDRESS_ID.eq(clientIpAddress.ID))
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
                        .and(targetServiceExposeMap.REMOVED.isNull()))
                .fetch().map(mapper);
    }

}
