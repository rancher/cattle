package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Subnet;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SampleDataStartupV1 extends AbstractSampleData {

    @Override
    protected String getName() {
        return "sampleDataVersion1";
    }

    @Override
    protected void populatedData(Account system, List<Object> toCreate) {
        Map<String, Object> networkData;
        try {
            networkData = jsonMapper.readValue("{\"libvirt\":{\"network\":{\"source\":[{\"bridge\":\"docker0\"}],\"type\":\"bridge\"}}}");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        Network network = createByUuid(Network.class, "unmanaged", NETWORK.ACCOUNT_ID, system.getId(), NETWORK.IS_PUBLIC, true, NETWORK.NAME,
                "Unmanaged Network", NETWORK.STATE, CommonStatesConstants.REQUESTED);

        toCreate.add(network);

        network = createByUuid(Network.class, "managed-docker0", NetworkConstants.FIELD_HOST_VNET_URI, "bridge://docker0",
                NetworkConstants.FIELD_DYNAMIC_CREATE_VNET, true, NETWORK.ACCOUNT_ID, system.getId(), NETWORK.IS_PUBLIC, true, NETWORK.KIND,
                NetworkConstants.KIND_HOSTONLY, NETWORK.NAME, "Managed Network on docker0", NETWORK.STATE, CommonStatesConstants.REQUESTED, NETWORK.DATA,
                networkData);

        toCreate.add(network);

        toCreate.add(createByUuid(Subnet.class, "docker0-subnet", SUBNET.ACCOUNT_ID, system.getId(), SUBNET.CIDR_SIZE, 16, SUBNET.KIND, "subnet",
                SUBNET.IS_PUBLIC, true, SUBNET.NAME, "Subnet for managed docker0", SUBNET.NETWORK_ADDRESS, "10.42.0.0", SUBNET.NETWORK_ID, network.getId(),
                SUBNET.STATE, CommonStatesConstants.REQUESTED));

        NetworkServiceProvider networkServiceProvider = createByUuid(NetworkServiceProvider.class, "docker0-agent-instance-provider",
                NETWORK_SERVICE_PROVIDER.ACCOUNT_ID, system.getId(), NETWORK_SERVICE_PROVIDER.KIND, NetworkServiceProviderConstants.KIND_AGENT_INSTANCE,
                NETWORK_SERVICE_PROVIDER.NAME, "Agent instance provider for managed docker0", NETWORK_SERVICE_PROVIDER.NETWORK_ID, network.getId(),
                NETWORK_SERVICE_PROVIDER.STATE, CommonStatesConstants.REQUESTED);

        toCreate.add(networkServiceProvider);

        toCreate.add(createByUuid(NetworkService.class, "docker0-dns-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_DNS, NETWORK_SERVICE.NAME, "DNS for managed docker0", NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(), NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-dhcp-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_DHCP, NETWORK_SERVICE.NAME, "DHCP for managed docker0", NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(), NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-link-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_LINK, NETWORK_SERVICE.NAME, "Instance links for managed docker0", NETWORK_SERVICE.NETWORK_ID, network.getId(),
                NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(), NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-ipsec-tunnel-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_IPSEC_TUNNEL, NETWORK_SERVICE.NAME, "IPsec tunnels for managed docker0", NETWORK_SERVICE.NETWORK_ID,
                network.getId(), NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(), NETWORK_SERVICE.STATE,
                CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-port-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_PORT_SERVICE, NETWORK_SERVICE.NAME, "Ports service for managed docker0", NETWORK_SERVICE.NETWORK_ID,
                network.getId(), NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(), NETWORK_SERVICE.STATE,
                CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-host-nat-gateway-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_HOST_NAT_GATEWAY, NETWORK_SERVICE.NAME, "Host nat gateway service for managed docker0",
                NETWORK_SERVICE.NETWORK_ID, network.getId(), NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(),
                NETWORK_SERVICE.STATE, CommonStatesConstants.REQUESTED));

        toCreate.add(createByUuid(NetworkService.class, "docker0-metadata-service", NETWORK_SERVICE.ACCOUNT_ID, system.getId(), NETWORK_SERVICE.KIND,
                NetworkServiceConstants.KIND_METADATA, NETWORK_SERVICE.NAME, "Meta data service for managed docker0", NETWORK_SERVICE.NETWORK_ID,
                network.getId(), NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, networkServiceProvider.getId(), NETWORK_SERVICE.STATE,
                CommonStatesConstants.REQUESTED));
    }

}
