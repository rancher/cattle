package io.cattle.platform.process.stack;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;

public class K8sStackRemove extends StackAgentHandler {

    public K8sStackRemove(AgentLocator agentLocator, ObjectSerializer factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
        commandName = "kubernetesStack.remove";
        stackKind = "kubernetesStack";
        agentRequired = false;
        agentService = "io.rancher.container.agent_service.kubernetes_stack";
    }

}