package io.cattle.platform.process.network;

import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class NetworkRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Network network = (Network)state.getResource();
        for (Subnet subnet : objectManager.children(network, Subnet.class)) {
            deactivateThenRemove(subnet, null);
        }

        return null;
    }

}