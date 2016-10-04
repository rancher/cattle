package io.cattle.platform.systemstack.listener;

import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SystemStackRemovePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    private static final Map<String, List<String>> STACKS_TO_CLEANUP = new HashMap<>();
    static {
        STACKS_TO_CLEANUP.put(SystemStackUpdate.KUBERNETES,
                Arrays.asList("kubernetes://", "kubernetes-loadbalancers://", "kubernetes-ingress-lbs://"));
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "stack.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Stack systemStack = (Stack) state.getResource();
        if (systemStack.getExternalId() == null) {
            return null;
        }

        String systemStackType = SystemStackUpdate.getStackTypeFromExternalId(systemStack.getExternalId());
        if (systemStackType == null) {
            return null;
        }

        for (Stack toCleanup : getStacksToCleanup(systemStack, systemStackType)) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, toCleanup, null);
        }
        return null;
    }

    protected List<Stack> getStacksToCleanup(Stack systemStack, String systemStackType) {
        List<Stack> all = objectManager.find(Stack.class, STACK.ACCOUNT_ID,
                systemStack.getAccountId(),
                STACK.REMOVED, null);
        List<Stack> toCleanup = new ArrayList<>();
        List<String> stackPrefixes = STACKS_TO_CLEANUP.get(systemStackType);
        if (stackPrefixes != null) {
            for (String prefix : stackPrefixes) {
                Iterator<Stack> it = all.iterator();
                while (it.hasNext()) {
                    Stack stack = it.next();
                    if (stack.getExternalId() == null) {
                        it.remove();
                        continue;
                    }
                    if (stack.getExternalId().startsWith(prefix)) {
                        toCleanup.add(stack);
                        it.remove();
                    }
                }
            }
        }
        return toCleanup;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
