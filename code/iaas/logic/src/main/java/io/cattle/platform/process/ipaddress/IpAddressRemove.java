package io.cattle.platform.process.ipaddress;

import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.IpAddressNicMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class IpAddressRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        for (IpAddressNicMap map : objectManager.children(state.getResource(), IpAddressNicMap.class)) {
            deactivateThenRemove(map, state.getData());
        }
        for (HostIpAddressMap map : objectManager.children(state.getResource(), HostIpAddressMap.class)) {
            deactivateThenRemove(map, state.getData());
        }
        return null;
    }

}
