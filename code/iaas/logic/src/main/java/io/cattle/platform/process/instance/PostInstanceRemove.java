package io.cattle.platform.process.instance;

import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;

import javax.inject.Inject;

public class PostInstanceRemove extends AgentBasedProcessHandler {

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instance.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();

        for (InstanceHostMap ihm : mapDao.findAll(InstanceHostMap.class, Instance.class, instance.getId())) {
            Object agentResource = getObjectByRelationship("host", ihm);
            if (ObjectUtils.getRemoved(agentResource) != null)
                continue; // Short circuit
            RemoteAgent agent = agentLocator.lookupAgent(agentResource);
            if (agent == null)
                continue;

            handleEvent(state, process, ihm, ihm, agentResource, agent);
        }

        return new HandlerResult(state.getData());
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }
}
