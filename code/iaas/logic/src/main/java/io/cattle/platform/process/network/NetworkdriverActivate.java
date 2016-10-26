package io.cattle.platform.process.network;

import static io.cattle.platform.core.model.tables.NetworkTable.*;

import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.constants.NetworkDriverConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkDriver;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NetworkdriverActivate extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
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

}