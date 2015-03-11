package io.cattle.iaas.cluster.process;

import io.cattle.iaas.cluster.service.ClusterManager;
import io.cattle.platform.core.model.ClusterHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ClusterHostMapCreateHandler extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    ClusterManager clusterManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "clusterhostmap.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Long clusterId = ((ClusterHostMap) state.getResource()).getClusterId();
        Host cluster = objectManager.loadResource(Host.class, clusterId);

        clusterManager.updateClusterServerConfig(state, cluster);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
