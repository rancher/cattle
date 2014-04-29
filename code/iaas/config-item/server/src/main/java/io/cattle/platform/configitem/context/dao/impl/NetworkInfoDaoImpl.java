package io.cattle.platform.configitem.context.dao.impl;

import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import io.cattle.platform.configitem.context.dao.NetworkInfoDao;
import io.cattle.platform.core.constants.IpAddressConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.tables.NicTable;
import io.cattle.platform.core.model.tables.records.NetworkRecord;
import io.cattle.platform.core.model.tables.records.NetworkServiceRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooq.impl.DSL;

public class NetworkInfoDaoImpl extends AbstractJooqDao implements NetworkInfoDao {

    @Override
    public List<?> networkClients(Instance instance) {
        NicTable instanceNic = NIC.as("instance_nic");
        NicTable clientNic = NIC.as("client_nic");

        return create()
                .select(
                        clientNic.DEVICE_NUMBER.as("clientDeviceNumber"),
                        clientNic.MAC_ADDRESS.as("macAddress"),
                        instanceNic.DEVICE_NUMBER.as("instanceDeviceNumber"),
                        INSTANCE.DOMAIN.as("instanceDomain"),
                        INSTANCE.HOSTNAME.as("hostname"),
                        INSTANCE.ID.as("instanceId"),
                        INSTANCE.UUID.as("instanceUuid"),
                        IP_ADDRESS.ADDRESS.as("ipAddress"),
                        NETWORK.DOMAIN.as("networkDomain"),
                        SUBNET.GATEWAY.as("gateway"),
                        DSL.val(DEFAULT_DOMAIN.get()).as("defaultDomain")
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
                .fetchMaps();
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

}
