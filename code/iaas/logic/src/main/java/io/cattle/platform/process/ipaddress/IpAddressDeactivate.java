package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.network.NetworkService;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class IpAddressDeactivate extends AbstractDefaultProcessHandler {

    @Inject
    NetworkService networkService;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        IpAddress ipAddress = (IpAddress) state.getResource();
        Network network = objectManager.loadResource(Network.class, ipAddress.getNetworkId());
        if (network == null) {
            return null;
        }

        networkService.releaseIpAddress(network, ipAddress);
        return null;
    }
}
