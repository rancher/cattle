package io.cattle.platform.process.stack;

import io.cattle.platform.core.model.Stack;

import javax.inject.Named;

@Named
public class K8sStackFinishupgrade extends StackAgentHandler {

    public K8sStackFinishupgrade() {
        setCommandName("kubernetesStack.finishupgrade");
        setDataTypeClass(Stack.class);
        setProcessNames("stack.finishupgrade");
        setStackKind("kubernetesStack");
        setAgentService("io.rancher.container.agent_service.kubernetes_stack");
        setPriority(DEFAULT);
    }

}
