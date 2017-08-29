package io.cattle.platform.process.instance;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessHandler;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.resource.ResourceMonitor;

public class InstanceStartCheck implements ProcessHandler {

    MetadataManager metadataManager;
    ObjectManager objectManager;
    ResourceMonitor resourceMonitor;

    public InstanceStartCheck(MetadataManager metadataManager, ObjectManager objectManager, ResourceMonitor resourceMonitor) {
        this.metadataManager = metadataManager;
        this.objectManager = objectManager;
        this.resourceMonitor = resourceMonitor;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (instance.getHostId() == null) {
            return null;
        }

        Metadata metadata = metadataManager.getMetadataForCluster(instance.getClusterId());
        for (HostInfo host : metadata.getHosts()) {
            if (host.getId() == instance.getHostId() && host.getAgentId() == null) {
                return waitForHost(host.getId());
            }
        }

        return null;
    }

    private HandlerResult waitForHost(long id) {
        Host host = objectManager.loadResource(Host.class, id);
        ListenableFuture<Host> future = resourceMonitor.waitFor(host, "agent available", (h) -> h.getAgentId() != null);
        return new HandlerResult(future);
    }

}
