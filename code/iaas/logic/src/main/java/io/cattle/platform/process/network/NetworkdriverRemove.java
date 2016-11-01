package io.cattle.platform.process.network;

import static io.cattle.platform.core.model.tables.NetworkTable.*;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.NetworkDriver;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class NetworkdriverRemove extends AbstractDefaultProcessHandler {

    @Inject
    JsonMapper jsonMapper;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        NetworkDriver networkDriver = (NetworkDriver)state.getResource();
        List<Network> networks = objectManager.find(Network.class,
                NETWORK.NETWORK_DRIVER_ID, networkDriver.getId(),
                NETWORK.REMOVED, null);
        for (Network network : networks) {
            deactivateThenScheduleRemove(network, null);
        }
        return null;
    }

}