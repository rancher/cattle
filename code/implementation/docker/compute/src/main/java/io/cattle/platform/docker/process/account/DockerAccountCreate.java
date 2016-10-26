package io.cattle.platform.docker.process.account;

import static io.cattle.platform.core.model.tables.NetworkTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.addon.ServicesPortRange;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicStringListProperty;

public class DockerAccountCreate extends AbstractObjectProcessLogic implements ProcessPostListener {
    DynamicStringListProperty KINDS = ArchaiusUtil.getList("docker.network.create.account.types");

    @Inject
    NetworkDao networkDao;

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    JsonMapper jsonMapper;

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

        createNetwork(NetworkConstants.KIND_DOCKER_HOST, account, networksByKind, "Docker Host Network Mode", null);
        createNetwork(NetworkConstants.KIND_DOCKER_NONE, account, networksByKind, "Docker None Network Mode", null);
        createNetwork(NetworkConstants.KIND_DOCKER_CONTAINER, account, networksByKind, "Docker Container Network Mode", null);
        createNetwork(NetworkConstants.KIND_DOCKER_BRIDGE, account, networksByKind, "Docker Bridge Network Mode", null);

        ServicesPortRange portRange = DataAccessor.field(account, AccountConstants.FIELD_PORT_RANGE, jsonMapper,
                ServicesPortRange.class);
        if (portRange == null) {
            portRange = AccountConstants.getDefaultServicesPortRange();
        }

        return new HandlerResult(AccountConstants.FIELD_PORT_RANGE, portRange).withShouldContinue(true);
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

        for (Network network : objectManager.find(Network.class,
                NETWORK.ACCOUNT_ID, account.getId(),
                NETWORK.REMOVED, null)) {
            result.put(network.getKind(), network);
        }

        return result;
    }
}
