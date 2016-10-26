package io.cattle.platform.process.network;

import static io.cattle.platform.core.model.tables.SubnetTable.*;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkDriverConstants;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkDriver;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.tables.records.SubnetRecord;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NetworkCreate extends AbstractDefaultProcessHandler {

    private static final String SUBNET_INDEX = "subnetIndex";

    @Inject
    JsonMapper jsonMapper;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Network network = (Network)state.getResource();
        Object obj = DataAccessor.field(network, NetworkConstants.FIELD_SUBNETS, Object.class);
        if (obj == null) {
            return null;
        }

        Map<Long, Subnet> existingSubnets = new HashMap<>();
        for (Subnet subnet : objectManager.children(network, Subnet.class)) {
            Long index = DataAccessor.fromDataFieldOf(subnet).withKey(SUBNET_INDEX).as(Long.class);
            if (index != null) {
                existingSubnets.put(index, subnet);
            }
        }

        List<? extends Subnet> subnets = jsonMapper.convertCollectionValue(obj, ArrayList.class, SubnetRecord.class);
        for (int i = 0 ; i < subnets.size() ; i++) {
            Long key = new Long(i);
            if (existingSubnets.containsKey(key)) {
                continue;
            }

            Subnet subnet = subnets.get(i);
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
            createThenActivate(subnet, null);
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

        return new HandlerResult(NetworkConstants.FIELD_METADATA, metadata).withShouldContinue(true);
    }

}