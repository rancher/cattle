package io.cattle.platform.process.stack;

import io.cattle.platform.core.model.Stack;

import java.util.Arrays;

import javax.inject.Named;

@Named
public class K8sStackUpgrade extends StackAgentHandler {

    public K8sStackUpgrade() {
        setCommandName("kubernetesStack.upgrade");
        setDataTypeClass(Stack.class);
        setProcessNames("stack.upgrade");
        setStackKind("kubernetesStack");
        setAgentService("io.rancher.container.agent_service.kubernetes_stack");
        setErrorChainProcess("stack.cancelupgrade");
        setPriority(DEFAULT);
        setProcessDataKeys(Arrays.asList(
                "templates",
                "environment",
                "externalId"));
    }

}
