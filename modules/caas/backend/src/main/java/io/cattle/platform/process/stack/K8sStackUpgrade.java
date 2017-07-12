package io.cattle.platform.process.stack;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;

import java.util.Arrays;

public class K8sStackUpgrade extends StackAgentHandler {

    public K8sStackUpgrade(AgentLocator agentLocator, ObjectSerializer factory, ObjectManager objectManager, ObjectProcessManager processManager) {
        super(agentLocator, factory, objectManager, processManager);
        commandName = "kubernetesStack.upgrade";
        stackKind = "kubernetesStack";
        agentService = "io.rancher.container.agent_service.kubernetes_stack";
        errorChainProcess = "stack.cancelupgrade";
        processDataKeys = Arrays.asList(
                "templates",
                "environment",
                "externalId");
    }

}
