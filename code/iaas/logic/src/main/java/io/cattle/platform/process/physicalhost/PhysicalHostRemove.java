package io.cattle.platform.process.physicalhost;

import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Named;

@Named
public class PhysicalHostRemove extends AbstractDefaultProcessHandler {

    @Override
    public HandlerResult handle(final ProcessState state, ProcessInstance process) {
        final PhysicalHost pHost = (PhysicalHost) state.getResource();

        for (Host host : getObjectManager().children(pHost, Host.class)) {
            deactivateThenRemove(host, state.getData());
        }

        return null;
    }
}
