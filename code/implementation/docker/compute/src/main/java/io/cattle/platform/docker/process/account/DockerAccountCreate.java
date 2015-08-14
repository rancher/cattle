package io.cattle.platform.docker.process.account;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringListProperty;

public class DockerAccountCreate extends AbstractObjectProcessLogic implements ProcessPostListener {
    DynamicStringListProperty KINDS = ArchaiusUtil.getList("docker.network.create.account.types");
    DynamicStringListProperty DOCKER_NETWORK_SUBNET_CIDR = ArchaiusUtil.getList("docker.network.subnet.cidr");
    DynamicStringListProperty DOCKER_VIP_SUBNET_CIDR = ArchaiusUtil.getList("docker.vip.subnet.cidr");


    @Inject
    NetworkDao networkDao;

    @Inject
    GenericResourceDao resourceDao;

    @Override
    public String[] getProcessNames() {
        return new String[]{"account.create"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Account account = (Account)state.getResource();
        if (!KINDS.get().contains(account.getKind())) {
            return null;
        }

        Map<String, Network> networksByKind = getNetworksByUuid(account);

        createNetwork(DockerNetworkConstants.KIND_DOCKER_HOST, account, networksByKind, "Docker Host Network Mode", null);
        createNetwork(DockerNetworkConstants.KIND_DOCKER_NONE, account, networksByKind, "Docker None Network Mode", null);
        createNetwork(DockerNetworkConstants.KIND_DOCKER_CONTAINER, account, networksByKind, "Docker Container Network Mode", null);
        createNetwork(DockerNetworkConstants.KIND_DOCKER_BRIDGE, account, networksByKind, "Docker Bridge Network Mode", null);

        Network managedNetwork = createManagedNetwork(account, networksByKind);
        return new HandlerResult(AccountConstants.FIELD_DEFAULT_NETWORK_ID, managedNetwork.getId()).withShouldContinue(true);
    }

    protected Network createManagedNetwork(Account account, Map<String, Network> networksByKind) {
        Network network = createNetwork(NetworkConstants.KIND_HOSTONLY, account, networksByKind,
                "Rancher Managed Network",
                NetworkConstants.FIELD_HOST_VNET_URI, "bridge://docker0",
                NetworkConstants.FIELD_DYNAMIC_CREATE_VNET, true);

        networkDao.addManagedNetworkSubnet(network);
        createAgentInstanceProvider(network);
        return network;
    }

    protected void createAgentInstanceProvider(Network network) {
        List<String> servicesKinds = new ArrayList<String>();
        servicesKinds.add(NetworkServiceConstants.KIND_DNS);
        servicesKinds.add(NetworkServiceConstants.KIND_LINK);
        servicesKinds.add(NetworkServiceConstants.KIND_IPSEC_TUNNEL);
        servicesKinds.add(NetworkServiceConstants.KIND_PORT_SERVICE);
        servicesKinds.add(NetworkServiceConstants.KIND_HOST_NAT_GATEWAY);
        servicesKinds.add(NetworkServiceConstants.KIND_HEALTH_CHECK);
        networkDao.createNsp(network, servicesKinds, NetworkServiceProviderConstants.KIND_AGENT_INSTANCE);
    }

    protected Network createNetwork(String kind, Account account, Map<String, Network> networksByKind,
                                  String name, String key, Object... valueKeyValue) {
        Network network = networksByKind.get(kind);
        if (network != null) {
            return network;
        }
        Map<String, Object> data = key == null ? new HashMap<String, Object>() :
                CollectionUtils.asMap(key, valueKeyValue);

        data.put(ObjectMetaDataManager.NAME_FIELD, name);
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, account.getId());
        data.put(ObjectMetaDataManager.KIND_FIELD, kind);

        return resourceDao.createAndSchedule(Network.class, data);
    }


    protected Map<String, Network> getNetworksByUuid(Account account) {
        Map<String, Network> result = new HashMap<>();

        for (Network network : networkDao.getNetworksForAccount(account.getId(), null)) {
            result.put(network.getKind(), network);
        }

        return result;
    }
}
