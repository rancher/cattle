package io.cattle.platform.process.stack;

import io.cattle.platform.core.model.Stack;

import javax.inject.Named;

@Named
public class K8sStackCreate extends StackAgentHandler {

    public K8sStackCreate() {
        setCommandName("kubernetesStack.create");
        setDataTypeClass(Stack.class);
        setProcessNames("stack.create");
        setStackKind("kubernetesStack");
        setAgentService("io.rancher.container.agent_service.kubernetes_stack");
        setErrorChainProcess("stack.error");
        setPriority(DEFAULT);
    }

}
