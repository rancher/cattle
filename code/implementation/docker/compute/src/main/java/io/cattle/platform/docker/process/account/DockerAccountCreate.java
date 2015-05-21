package io.cattle.platform.docker.process.account;

import static io.cattle.platform.core.model.tables.NetworkServiceProviderTable.*;
import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.constants.NetworkServiceProviderConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.docker.constants.DockerNetworkConstants;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;

import com.netflix.config.DynamicStringListProperty;

public class DockerAccountCreate extends AbstractObjectProcessLogic implements ProcessPostListener {

    DynamicStringListProperty KINDS = ArchaiusUtil.getList("docker.network.create.account.types");

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

        Network network = createNetwork(NetworkConstants.KIND_HOSTONLY, account, networksByKind, "Rancher Managed Network",
                NetworkConstants.FIELD_HOST_VNET_URI, "bridge://docker0",
                NetworkConstants.FIELD_DYNAMIC_CREATE_VNET, true);

        addSubnet(network);
        NetworkServiceProvider nsp = addNsp(network);

        Map<String, NetworkService> services = collectionNetworkServices(network);
        addService(services, nsp, NetworkServiceConstants.KIND_DNS);
        addService(services, nsp, NetworkServiceConstants.KIND_LINK);
        addService(services, nsp, NetworkServiceConstants.KIND_IPSEC_TUNNEL);
        addService(services, nsp, NetworkServiceConstants.KIND_PORT_SERVICE);
        addService(services, nsp, NetworkServiceConstants.KIND_HOST_NAT_GATEWAY);
        addService(services, nsp, NetworkServiceConstants.KIND_HEALTH_CHECK);

        createNetwork(DockerNetworkConstants.KIND_DOCKER_HOST, account, networksByKind, "Docker Host Network Mode", null);
        createNetwork(DockerNetworkConstants.KIND_DOCKER_NONE, account, networksByKind, "Docker None Network Mode", null);
        createNetwork(DockerNetworkConstants.KIND_DOCKER_CONTAINER, account, networksByKind, "Docker Container Network Mode", null);
        createNetwork(DockerNetworkConstants.KIND_DOCKER_BRIDGE, account, networksByKind, "Docker Bridge Network Mode", null);

        return new HandlerResult(AccountConstants.FIELD_DEFAULT_NETWORK_ID, network.getId()).withShouldContinue(true);
    }

    protected void addSubnet(Network network) {
        List<Subnet> subnets = objectManager.children(network, Subnet.class);
        if (subnets.size() > 0) {
            return;
        }

        resourceDao.createAndSchedule(Subnet.class,
                SUBNET.ACCOUNT_ID, network.getAccountId(),
                SUBNET.CIDR_SIZE, 16,
                SUBNET.NETWORK_ADDRESS, "10.42.0.0",
                SUBNET.NETWORK_ID, network.getId());
    }

    protected NetworkServiceProvider addNsp(Network network) {
        List<NetworkServiceProvider> nsp = objectManager.children(network, NetworkServiceProvider.class);

        if (nsp.size() > 0) {
            return nsp.get(0);
        }

        return resourceDao.createAndSchedule(NetworkServiceProvider.class,
                NETWORK_SERVICE_PROVIDER.ACCOUNT_ID, network.getAccountId(),
                NETWORK_SERVICE_PROVIDER.KIND, NetworkServiceProviderConstants.KIND_AGENT_INSTANCE,
                NETWORK_SERVICE_PROVIDER.NETWORK_ID, network.getId());
    }

    protected Network createNetwork(String kind, Account account, Map<String, Network> networksByKind,
                                  String name, String key, Object... valueKeyValue) {
        Network network = networksByKind.get(kind);
        Map<String, Object> data = key == null ? new HashMap<String, Object>() :
                CollectionUtils.asMap(key, valueKeyValue);

        data.put(ObjectMetaDataManager.NAME_FIELD, name);
        data.put(ObjectMetaDataManager.ACCOUNT_FIELD, account.getId());
        data.put(ObjectMetaDataManager.KIND_FIELD, kind);

        if (network == null) {
            network = resourceDao.createAndSchedule(Network.class, data);
        } else {
            network = objectManager.setFields(network, data);
        }

        return network;
    }

    protected Map<String, NetworkService> collectionNetworkServices(Network network) {
        Map<String, NetworkService> services = new HashMap<>();

        for (NetworkService service : objectManager.children(network, NetworkService.class)) {
            services.put(service.getKind(), service);
        }

        return services;
    }

    protected void addService(Map<String, NetworkService> services, NetworkServiceProvider nsp, String kind,
                              Object... keyValue) {
        if (services.containsKey(kind)) {
            return;
        }

        Map<Object, Object> data = new HashMap<>();
        if (keyValue != null && keyValue.length > 1) {
            data = CollectionUtils.asMap(keyValue[0], ArrayUtils.subarray(keyValue, 1, keyValue.length));
        }

        data.put(NETWORK_SERVICE.KIND, kind);
        data.put(NETWORK_SERVICE.ACCOUNT_ID, nsp.getAccountId());
        data.put(NETWORK_SERVICE.NETWORK_ID, nsp.getNetworkId());
        data.put(NETWORK_SERVICE.NETWORK_SERVICE_PROVIDER_ID, nsp.getId());

        resourceDao.createAndSchedule(NetworkService.class,
                objectManager.convertToPropertiesFor(NetworkService.class, data));
    }

    protected Map<String, Network> getNetworksByUuid(Account account) {
        Map<String, Network> result = new HashMap<>();

        for (Network network : networkDao.getNetworksForAccount(account.getId(), null)) {
            result.put(network.getKind(), network);
        }

        return result;
    }
}
