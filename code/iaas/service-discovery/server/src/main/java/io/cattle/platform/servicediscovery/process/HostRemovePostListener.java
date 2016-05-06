package io.cattle.platform.servicediscovery.process;

import io.cattle.platform.allocator.service.CacheManager;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

public class HostRemovePostListener extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "host.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Host host = (Host) state.getResource();
        CacheManager cm = CacheManager.getCacheManagerInstance(this.objectManager);
        cm.removeHostInfo(host.getId());

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
