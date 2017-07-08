package io.cattle.platform.process.network;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkDriverConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkDriver;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.lock.DefaultNetworkLock;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.ProxyUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.model.tables.NetworkTable.*;
import static io.cattle.platform.core.model.tables.SubnetTable.*;

public class NetworkProcessManager {

    private static final String SUBNET_INDEX = "subnetIndex";

    GenericResourceDao resourceDao;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    NetworkDao networkDao;
    LockManager lockManager;
    JsonMapper jsonMapper;
    ResourcePoolManager resourcePoolManager;

    public NetworkProcessManager(GenericResourceDao resourceDao, ObjectManager objectManager, ObjectProcessManager processManager, NetworkDao networkDao, LockManager lockManager, JsonMapper jsonMapper, ResourcePoolManager resourcePoolManager) {
        this.resourceDao = resourceDao;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.networkDao = networkDao;
        this.lockManager = lockManager;
        this.jsonMapper = jsonMapper;
        this.resourcePoolManager = resourcePoolManager;
    }

    public HandlerResult create(ProcessState state, ProcessInstance process) {
        Network network = (Network)state.getResource();
        Object obj = DataAccessor.field(network, NetworkConstants.FIELD_SUBNETS, Object.class);
        if (obj != null) {
            Map<Long, Subnet> existingSubnets = new HashMap<>();
            for (Subnet subnet : objectManager.children(network, Subnet.class)) {
                Long index = DataAccessor.fromDataFieldOf(subnet).withKey(SUBNET_INDEX).as(Long.class);
                if (index != null) {
                    existingSubnets.put(index, subnet);
                }
            }

            List<? extends Map<String, Object>> subnets = jsonMapper.convertCollectionValue(obj, ArrayList.class, Map.class);
            for (int i = 0 ; i < subnets.size() ; i++) {
                Long key = new Long(i);
                if (existingSubnets.containsKey(key)) {
                    continue;
                }

                Subnet subnet = ProxyUtils.proxy(subnets.get(i), Subnet.class);
                subnet = objectManager.create(Subnet.class,
                        SUBNET.NAME, subnet.getName(),
                        SUBNET.DESCRIPTION, subnet.getDescription(),
                        SUBNET.CIDR_SIZE, subnet.getCidrSize(),
                        SUBNET.END_ADDRESS, subnet.getEndAddress(),
                        SUBNET.GATEWAY, subnet.getGateway(),
                        SUBNET.NETWORK_ADDRESS, subnet.getNetworkAddress(),
                        SUBNET.NETWORK_ID, network.getId(),
                        SUBNET.START_ADDRESS, subnet.getStartAddress(),
                        SUBNET.DATA, CollectionUtils.asMap(SUBNET_INDEX, key),
                        SUBNET.ACCOUNT_ID, network.getAccountId());

                existingSubnets.put(key, subnet);
            }

            for (Subnet subnet : existingSubnets.values()) {
                processManager.executeCreateThenActivate(subnet, null);
            }
        }

        NetworkDriver driver = objectManager.loadResource(NetworkDriver.class, network.getNetworkDriverId());
        if (driver == null) {
            return null;
        }

        Map<String, Object> metadata = DataAccessor.fieldMap(network, NetworkConstants.FIELD_METADATA);
        Map<String, Object> driverMetadata = DataAccessor.fieldMap(driver, NetworkDriverConstants.FIELD_NETWORK_METADATA);
        Map<String, Object> cniConf = DataAccessor.fieldMap(driver, NetworkDriverConstants.FIELD_CNI_CONFIG);
        metadata.putAll(driverMetadata);
        metadata.put(NetworkDriverConstants.FIELD_CNI_CONFIG, cniConf);

        return new HandlerResult(NetworkConstants.FIELD_METADATA, metadata);
    }

    public HandlerResult activate(ProcessState state, ProcessInstance process) {
        Network network = (Network) state.getResource();

        String field = DataAccessor.field(network, NetworkConstants.FIELD_MAC_PREFIX, String.class);

        if (StringUtils.isBlank(field)) {
            PooledResource mac = resourcePoolManager.allocateOneResource(ResourcePoolManager.GLOBAL, network, new PooledResourceOptions()
                    .withQualifier(ResourcePoolConstants.MAC_PREFIX));
            if (mac == null) {
                throw new ExecutionException("Mac prefix allocation error", "Failed to get mac prefix", network);
            }
            field = mac.getName();
        }

        return new HandlerResult(NetworkConstants.FIELD_MAC_PREFIX, field);
    }


    public HandlerResult updateDefaultNetwork(ProcessState state, ProcessInstance process) {
        final Network network = (Network)state.getResource();
        lockManager.lock(new DefaultNetworkLock(network), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                setDefaultNetwork(network.getAccountId());
            }
        });
        return null;
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        Network network = (Network)state.getResource();
        for (Subnet subnet : objectManager.children(network, Subnet.class)) {
            processManager.executeDeactivateThenRemove(subnet, null);
        }

        resourcePoolManager.releaseResource(ResourcePoolManager.GLOBAL, network, new PooledResourceOptions().withQualifier(ResourcePoolConstants.MAC_PREFIX));
        return new HandlerResult(NetworkConstants.FIELD_MAC_PREFIX, new Object[] { null });
    }

    protected void setDefaultNetwork(Long accountId) {
        Account account = objectManager.loadResource(Account.class, accountId);
        if (account == null) {
            return;
        }

        Long defaultNetworkId = account.getDefaultNetworkId();
        Long newDefaultNetworkId = null;
        for (Network network : networkDao.getActiveNetworks(account.getId())) {
            if (network.getKind().startsWith(NetworkConstants.PREFIX_KIND_DOCKER) ||
                    network.getKind().equals("hostOnlyNetwork")) {
                continue;
            }

            if (network.getId().equals(defaultNetworkId)) {
                newDefaultNetworkId = defaultNetworkId;
                break;
            }

            if ((CommonStatesConstants.ACTIVATING.equals(network.getState()) ||
                    CommonStatesConstants.UPDATING_ACTIVE.equals(network.getState())) &&
                    newDefaultNetworkId == null) {
                newDefaultNetworkId = network.getId();
            } else if (CommonStatesConstants.ACTIVE.equals(network.getState())) {
                newDefaultNetworkId = network.getId();
            }
        }

        if (!Objects.equals(defaultNetworkId, newDefaultNetworkId)) {
            objectManager.setFields(account, AccountConstants.FIELD_DEFAULT_NETWORK_ID, newDefaultNetworkId);
        }
    }

    public HandlerResult networkDriverActivate(ProcessState state, ProcessInstance process) {
        NetworkDriver networkDriver = (NetworkDriver)state.getResource();
        List<Network> created = objectManager.children(networkDriver, Network.class);
        Map<String, Object> network = DataAccessor.fieldMap(networkDriver, NetworkDriverConstants.FIELD_DEFAULT_NETWORK);
        if (created.size() > 0 || network.size() == 0) {
            return null;
        }

        Map<Object, Object> props = new HashMap<>();
        props.putAll(network);
        props.put(NETWORK.ACCOUNT_ID, networkDriver.getAccountId());
        props.put(NETWORK.NETWORK_DRIVER_ID, networkDriver.getId());

        Map<String, Object> cniConf = DataAccessor.fieldMap(networkDriver, NetworkDriverConstants.FIELD_CNI_CONFIG);
        if (cniConf.size() > 0) {
            props.put(NETWORK.KIND, NetworkConstants.KIND_CNI);
        }

        resourceDao.createAndSchedule(Network.class, objectManager.convertToPropertiesFor(Network.class, props));
        return null;
    }

    public HandlerResult networkDriverRemove(ProcessState state, ProcessInstance process) {
        NetworkDriver networkDriver = (NetworkDriver)state.getResource();
        List<Network> networks = objectManager.find(Network.class,
                NETWORK.NETWORK_DRIVER_ID, networkDriver.getId(),
                NETWORK.REMOVED, null);
        for (Network network : networks) {
            processManager.executeDeactivateThenScheduleRemove(network, null);
        }
        return null;
    }

}
