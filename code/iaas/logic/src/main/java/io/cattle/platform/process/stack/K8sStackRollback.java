package io.cattle.platform.process.stack;

import io.cattle.platform.core.model.Stack;

import javax.inject.Named;

@Named
public class K8sStackRollback extends StackAgentHandler {

    public K8sStackRollback() {
        setCommandName("kubernetesStack.rollback");
        setDataTypeClass(Stack.class);
        setProcessNames("stack.rollback");
        setStackKind("kubernetesStack");
        setAgentService("io.rancher.container.agent_service.kubernetes_stack");
        setPriority(DEFAULT);
    }

}
