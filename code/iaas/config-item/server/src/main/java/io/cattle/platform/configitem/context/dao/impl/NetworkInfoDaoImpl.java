package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.IpAssociationTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.PhysicalHostTable.*;
import static io.cattle.platform.core.model.tables.PortTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import static io.cattle.platform.core.model.tables.VnetTable.*;
import static io.cattle.platform.core.constants.NetworkServiceConstants.*;

import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.configitem.context.data.ClientIpsecTunnelInfo;
import io.cattle.platform.configitem.context.data.HostPortForwardData;
import io.cattle.platform.configitem.context.data.HostRouteData;
import io.cattle.platform.configitem.context.data.IpAssociationData;
import io.cattle.platform.configitem.context.data.NetworkClientData;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Vnet;
import io.cattle.platform.core.model.tables.HostTable;
import io.cattle.platform.core.model.tables.InstanceTable;
import io.cattle.platform.core.model.tables.IpAddressTable;
import io.cattle.platform.core.model.tables.NetworkServiceTable;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.PhysicalHostTable;
import io.cattle.platform.core.model.tables.PortTable;
import io.cattle.platform.core.model.tables.SubnetTable;
import io.cattle.platform.core.model.tables.VnetTable;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.db.jooq.mapper.AggregateMultiRecordMapper;
import io.cattle.platform.db.jooq.mapper.MultiRecordMapper;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

public class NetworkInfoDaoImpl extends AbstractJooqDao implements NetworkInfoDao {

    NetworkDao networkDao;
    JsonMapper jsonMapper;
    ObjectManager objectManager;

    @Override
    public List<NetworkClientData> networkClients(Instance instance) {
        NicTable instanceNic = NIC.as("instance_nic");
        NicTable clientNic = NIC.as("client_nic");

        return create()
                .select(
                        clientNic.DEVICE_NUMBER.as("client_device_number"),
                        clientNic.MAC_ADDRESS.as("mac_address"),
                        instanceNic.DEVICE_NUMBER.as("instance_device_number"),
                        INSTANCE.DOMAIN.as("instance_domain"),
                        INSTANCE.HOSTNAME.as("hostname"),
                        INSTANCE.ID.as("instance_id"),
                        INSTANCE.UUID.as("instance_uuid"),
                        IP_ADDRESS.ADDRESS.as("ip_address"),
                        NETWORK.DOMAIN.as("network_domain"),
                        SUBNET.GATEWAY.as("gateway")
                )
                .from(instanceNic)
                .join(NETWORK)
                    .on(NETWORK.ID.eq(instanceNic.NETWORK_ID))
                .join(clientNic)
                    .on(NETWORK.ID.eq(clientNic.NETWORK_ID))
                .join(INSTANCE)
                    .on(clientNic.INSTANCE_ID.eq(INSTANCE.ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(clientNic.ID))
                .join(IP_ADDRESS)
                    .on(IP_ADDRESS.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID)
                        .and(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PRIMARY)))
                .join(SUBNET)
                    .on(IP_ADDRESS.SUBNET_ID.eq(SUBNET.ID))
                .where(instanceNic.INSTANCE_ID.eq(instance.getId())
                        .and(IP_ADDRESS.REMOVED.isNull())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(instanceNic.REMOVED.isNull()))
                .fetchInto(NetworkClientData.class);
    }

    @Override
    public List<NetworkClientData> vnetClients(Instance instance) {
        NicTable instanceNic = NIC.as("instance_nic");
        NicTable clientNic = NIC.as("client_nic");

        return create()
                .select(
                        clientNic.DEVICE_NUMBER.as("client_device_number"),
                        clientNic.MAC_ADDRESS.as("mac_address"),
                        instanceNic.DEVICE_NUMBER.as("instance_device_number"),
                        INSTANCE.DOMAIN.as("instance_domain"),
                        INSTANCE.HOSTNAME.as("hostname"),
                        INSTANCE.ID.as("instance_id"),
                        INSTANCE.UUID.as("instance_uuid"),
                        IP_ADDRESS.ADDRESS.as("ip_address"),
                        SUBNET.GATEWAY.as("gateway")
                )
                .from(instanceNic)
                .join(VNET)
                    .on(VNET.ID.eq(instanceNic.VNET_ID))
                .join(clientNic)
                    .on(VNET.ID.eq(clientNic.VNET_ID))
                .join(INSTANCE)
                    .on(clientNic.INSTANCE_ID.eq(INSTANCE.ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(clientNic.ID))
                .join(IP_ADDRESS)
                    .on(IP_ADDRESS.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID)
                        .and(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PRIMARY)))
                .join(SUBNET)
                    .on(IP_ADDRESS.SUBNET_ID.eq(SUBNET.ID))
                .where(instanceNic.INSTANCE_ID.eq(instance.getId())
                        .and(IP_ADDRESS.REMOVED.isNull())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(INSTANCE.REMOVED.isNull())
                        .and(clientNic.REMOVED.isNull())
                        .and(instanceNic.REMOVED.isNull()))
                .fetchInto(NetworkClientData.class);
    }

    @Override
    public List<? extends NetworkService> networkServices(Instance instance) {
        return create()
                .select(NETWORK_SERVICE.fields())
                .from(NETWORK_SERVICE)
                .join(NIC)
                    .on(NIC.NETWORK_ID.eq(NETWORK_SERVICE.NETWORK_ID))
                .join(NETWORK_SERVICE_PROVIDER)
                    .on(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID.eq(NETWORK_SERVICE_PROVIDER.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NETWORK_SERVICE_PROVIDER.KIND.eq(NetworkServiceProviderConstants.KIND_AGENT_INSTANCE)))
                .fetchInto(NetworkServiceRecord.class);
    }

    @Override
    public List<? extends NetworkService> networkServices(Agent agent) {
        return create()
                .select(NETWORK_SERVICE.fields())
                .from(NETWORK_SERVICE)
                .join(NIC)
                    .on(NIC.NETWORK_ID.eq(NETWORK_SERVICE.NETWORK_ID))
                .join(INSTANCE)
                    .on(INSTANCE.ID.eq(NIC.INSTANCE_ID))
                .join(NETWORK_SERVICE_PROVIDER)
                    .on(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID.eq(NETWORK_SERVICE_PROVIDER.ID))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(NIC.INSTANCE_ID))
                .join(HOST)
                    .on(HOST.ID.eq(INSTANCE_HOST_MAP.HOST_ID))
                .where(HOST.AGENT_ID.eq(agent.getId())
                        .and(NETWORK_SERVICE_PROVIDER.KIND.eq(NetworkServiceProviderConstants.KIND_AGENT_INSTANCE))
                        .and(INSTANCE.AGENT_ID.isNotNull())
                        .and(INSTANCE.REMOVED.isNull()))
                .fetchInto(NetworkServiceRecord.class);
    }

    @Override
    public Map<Long, Network> networks(Instance instance) {
        final Map<Long, Network> result = new HashMap<Long, Network>();

        List<NetworkRecord> records = create()
                .select(NETWORK.fields())
                .from(NETWORK)
                .join(NIC)
                    .on(NETWORK.ID.eq(NIC.NETWORK_ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId()))
                .fetchInto(NetworkRecord.class);

        for ( NetworkRecord network : records ) {
            result.put(network.getId(), network);
        }

        return result;
    }

    @Override
    public List<ClientIpsecTunnelInfo> getIpsecTunnelClient(Instance inputAgentInstance) {
        Nic primaryNic = networkDao.getPrimaryNic(inputAgentInstance.getId());
        final Map<Long,ClientIpsecTunnelInfo> physicalHostToAgentInstance = new HashMap<Long, ClientIpsecTunnelInfo>();

        MultiRecordMapper<ClientIpsecTunnelInfo> mapper = new MultiRecordMapper<ClientIpsecTunnelInfo>() {
            @Override
            protected ClientIpsecTunnelInfo map(List<Object> input) {
                Instance instance = (Instance)input.get(0);
                Host host = (Host)input.get(1);
                IpAddress ipAddress = (IpAddress)input.get(2);

                ClientIpsecTunnelInfo info = new ClientIpsecTunnelInfo();
                info.setHost(host);
                info.setInstance(instance);
                info.setHostIpAddress(ipAddress);
                info.setIpAddress((IpAddress)input.get(3));
                info.setSubnet((Subnet)input.get(4));
                info.setPhysicalHost((PhysicalHost)input.get(5));

                if ( instance.getAgentId() != null ) {
                    Map<?,?> map = DataAccessor.fromDataFieldOf(instance)
                                            .withScopeKey("ipsec")
                                            .withKey(Long.toString(host.getId()))
                                            .withDefault(new HashMap<String,Object>())
                                            .as(jsonMapper, Map.class);

                    Object natPort = map.get("nat");
                    Object isaKmpPort = map.get("isakmp");

                    if ( natPort instanceof Number && isaKmpPort instanceof Number ) {
                        info.setNatPort(((Number)natPort).intValue());
                        info.setIsaKmpPort(((Number)isaKmpPort).intValue());
                        info.setAgentInstance(instance);

                        physicalHostToAgentInstance.put(host.getPhysicalHostId(), info);
                    }
                }

                return info;
            }
        };

        InstanceTable instance = mapper.add(INSTANCE);
        HostTable host = mapper.add(HOST);
        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        IpAddressTable clientIpAddress = mapper.add(IP_ADDRESS);
        SubnetTable subnet = mapper.add(SUBNET);
        PhysicalHostTable physicalHost = mapper.add(PHYSICAL_HOST);

        List<ClientIpsecTunnelInfo> tempResult = create()
                .select(mapper.fields())
                .from(instance)
                .join(NIC)
                    .on(NIC.INSTANCE_ID.eq(instance.ID))
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(instance.ID))
                .join(host)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(host.ID))
                .leftOuterJoin(physicalHost)
                    .on(physicalHost.ID.eq(host.PHYSICAL_HOST_ID))
                .leftOuterJoin(HOST_IP_ADDRESS_MAP)
                    .on(HOST_IP_ADDRESS_MAP.HOST_ID.eq(host.ID))
                .leftOuterJoin(ipAddress)
                    .on(ipAddress.ID.eq(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .join(clientIpAddress)
                    .on(clientIpAddress.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                .join(subnet)
                    .on(subnet.ID.eq(clientIpAddress.SUBNET_ID))
                .where(NIC.NETWORK_ID.eq(primaryNic.getNetworkId())
                        .and(clientIpAddress.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(NIC.REMOVED.isNull())
                        .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(clientIpAddress.REMOVED.isNull())
                        .and(ipAddress.REMOVED.isNull())
                        .and(host.REMOVED.isNull())
                        .and(subnet.REMOVED.isNull())
                        .and(instance.REMOVED.isNull()))
                .fetch().map(mapper);

        List<ClientIpsecTunnelInfo> result = new ArrayList<ClientIpsecTunnelInfo>(tempResult.size());

        for ( ClientIpsecTunnelInfo info : tempResult ) {
            if ( info == null ) {
                continue;
            }

            ClientIpsecTunnelInfo agentInfo = physicalHostToAgentInstance.get(info.getHost().getPhysicalHostId());
            if ( agentInfo == null ) {
                continue;
            }

            info.setAgentInstance(agentInfo.getAgentInstance());
            info.setNatPort(agentInfo.getNatPort());
            info.setIsaKmpPort(agentInfo.getIsaKmpPort());

            if ( info.getHostIpAddress() == null ) {
                info.setHostIpAddress(agentInfo.getHostIpAddress());
            }

            if ( info.getHostIpAddress() != null ) {
                result.add(info);
            }
        }

        return result;
    }

    @Override
    public List<HostPortForwardData> getHostPorts(Agent agent) {
        MultiRecordMapper<HostPortForwardData> mapper = new MultiRecordMapper<HostPortForwardData>() {
            @Override
            protected HostPortForwardData map(List<Object> input) {
                HostPortForwardData data = new HostPortForwardData();
                data.setPrivateIpAddress((IpAddress)input.get(0));
                data.setPublicIpAddress((IpAddress)input.get(1));
                data.setPort((Port)input.get(2));

                return data;
            }
        };

        IpAddressTable privateIpAddress = mapper.add(IP_ADDRESS);
        IpAddressTable publicIpAddress = mapper.add(IP_ADDRESS);
        PortTable port = mapper.add(PORT);

        return create()
            .select(mapper.fields())
            .from(INSTANCE_HOST_MAP)
            .join(HOST)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
            .join(port)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(port.INSTANCE_ID))
            .join(privateIpAddress)
                .on(port.PRIVATE_IP_ADDRESS_ID.eq(privateIpAddress.ID))
            .join(publicIpAddress)
                .on(port.PUBLIC_IP_ADDRESS_ID.eq(publicIpAddress.ID))
            .join(NIC)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(NIC.INSTANCE_ID))
            .join(NETWORK_SERVICE)
                .on(NIC.NETWORK_ID.eq(NETWORK_SERVICE.NETWORK_ID).and(NETWORK_SERVICE.KIND.eq(KIND_PORT_SERVICE)))
            .where(HOST.AGENT_ID.eq(agent.getId())
                    .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                    .and(port.REMOVED.isNull())
                    .and(port.PUBLIC_PORT.isNotNull())
                    .and(port.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE, CommonStatesConstants.UPDATING_ACTIVE))
                    .and(HOST.REMOVED.isNull()))
                    .and(NIC.REMOVED.isNull())
                    .and(NETWORK_SERVICE.REMOVED.isNull())
            .orderBy(port.CREATED.asc())
            .fetch().map(mapper);
    }

    @Override
    public List<IpAssociationData> getHostIps(Agent agent) {
        AggregateMultiRecordMapper<IpAssociationData> mapper = new AggregateMultiRecordMapper<IpAssociationData>(IpAssociationData.class);

        IpAddressTable ipAddress = mapper.add(IP_ADDRESS);
        IpAddressTable targetIpAddress = mapper.add(IP_ADDRESS);
        SubnetTable subnet = mapper.add(SUBNET);

        return create()
                .select(mapper.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(NIC)
                    .on(NIC.INSTANCE_ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .join(targetIpAddress)
                    .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(IP_ASSOCIATION)
                    .on(IP_ASSOCIATION.CHILD_IP_ADDRESS_ID.eq(targetIpAddress.ID))
                .join(ipAddress)
                    .on(ipAddress.ID.eq(IP_ASSOCIATION.IP_ADDRESS_ID))
                .join(subnet)
                    .on(subnet.ID.eq(targetIpAddress.SUBNET_ID))
                .where(HOST.AGENT_ID.eq(agent.getId())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE))
                        .and(ipAddress.REMOVED.isNull())
                        .and(IP_ASSOCIATION.REMOVED.isNull())
                        .and(IP_ASSOCIATION.STATE.in(CommonStatesConstants.ACTIVATING, CommonStatesConstants.ACTIVE))
                        .and(ipAddress.ADDRESS.isNotNull())
                        .and(targetIpAddress.ADDRESS.isNotNull())
                        .and(NIC.REMOVED.isNull()))
                .fetch().map(mapper);
    }

    @Override
    public List<HostRouteData> getHostRoutes(Agent agent) {
        MultiRecordMapper<HostRouteData> mapper = new MultiRecordMapper<HostRouteData>() {
            @Override
            protected HostRouteData map(List<Object> input) {
                HostRouteData data = new HostRouteData();
                data.setInstance((Instance)input.get(0));
                data.setSubnet((Subnet)input.get(1));
                data.setHostNatGatewayService((NetworkService)input.get(2));
                data.setVnet((Vnet)input.get(3));

                return data;
            }
        };

        InstanceTable instance = mapper.add(INSTANCE);
        SubnetTable subnet = mapper.add(SUBNET);
        NetworkServiceTable networkService = mapper.add(NETWORK_SERVICE);
        VnetTable vnet = mapper.add(VNET);

        return create()
                .select(mapper.fields())
                .from(HOST)
                .join(INSTANCE_HOST_MAP)
                    .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
                .join(instance)
                    .on(instance.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
                .join(NIC)
                    .on(NIC.INSTANCE_ID.eq(instance.ID))
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .join(IP_ADDRESS)
                    .on(IP_ADDRESS.ID.eq(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID))
                .join(subnet)
                    .on(subnet.ID.eq(IP_ADDRESS.SUBNET_ID))
                .leftOuterJoin(vnet)
                    .on(vnet.ID.eq(NIC.VNET_ID))
                .leftOuterJoin(networkService)
                    .on(networkService.NETWORK_ID.eq(NIC.NETWORK_ID)
                            .and(networkService.KIND.eq(NetworkServiceConstants.KIND_HOST_NAT_GATEWAY)))
                .where(HOST.AGENT_ID.eq(agent.getId())
                        .and(IP_ADDRESS.ROLE.eq(IpAddressConstants.ROLE_PRIMARY))
                        .and(HOST.REMOVED.isNull())
                        .and(INSTANCE_HOST_MAP.REMOVED.isNull())
                        .and(NIC.REMOVED.isNull())
                        .and(instance.REMOVED.isNull())
                        .and(IP_ADDRESS_NIC_MAP.REMOVED.isNull())
                        .and(subnet.REMOVED.isNull()))
                .fetch().map(mapper);
    }


    @Override
    public Map<Nic, Subnet> getNicsAndSubnet(Instance instance) {
        final Map<Nic, Subnet> result = new HashMap<Nic, Subnet>();

        MultiRecordMapper<List<Object>> mapper = new MultiRecordMapper<List<Object>>() {
            @Override
            protected List<Object> map(List<Object> input) {
                result.put((Nic)input.get(0), (Subnet)input.get(1));
                return input;
            }
        };

        NicTable nic = mapper.add(NIC);
        SubnetTable subnet = mapper.add(SUBNET);

        create()
            .select(mapper.fields())
            .from(nic)
            .leftOuterJoin(subnet)
                .on(nic.SUBNET_ID.eq(subnet.ID))
            .where(nic.INSTANCE_ID.eq(instance.getId()))
            .fetch().map(mapper);

        return result;
    }

    @Inject
    public void setNetworkDao(NetworkDao networkDao) {
        this.networkDao = networkDao;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
