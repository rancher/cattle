package io.cattle.platform.process.stack;

import io.cattle.platform.core.model.Stack;

import javax.inject.Named;

@Named
public class K8sStackRemove extends StackAgentHandler {

    public K8sStackRemove() {
        setCommandName("kubernetesStack.remove");
        setDataTypeClass(Stack.class);
        setProcessNames("stack.remove");
        setStackKind("kubernetesStack");
        setAgentService("io.rancher.container.agent_service.kubernetes_stack");
        setPriority(DEFAULT);
    }

}