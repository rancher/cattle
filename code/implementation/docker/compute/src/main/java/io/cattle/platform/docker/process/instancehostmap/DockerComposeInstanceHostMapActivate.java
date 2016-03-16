package io.cattle.platform.docker.process.instancehostmap;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.docker.service.ComposeManager;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import javax.inject.Inject;

public class DockerComposeInstanceHostMapActivate extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {

    @Inject
    ComposeManager composeManager;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancehostmap.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceHostMap map = (InstanceHostMap)state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());
        composeManager.setupServiceAndInstance(instance);
        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT + 1;
    }



}