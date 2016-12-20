package io.cattle.platform.systemstack.process;

import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.systemstack.listener.SystemStackUpdate;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

@Named
public class SystemStackRemovePostHandler extends AbstractObjectProcessLogic implements ProcessPostListener,
        Priority {

    private static final Map<String, List<String>> STACKS_TO_CLEANUP_EXTERNAL_ID = new HashMap<>();
    private static final Map<String, List<String>> STACKS_TO_CLEANUP_KIND = new HashMap<>();
    static {
        STACKS_TO_CLEANUP_EXTERNAL_ID.put(SystemStackUpdate.KUBERNETES,
                Arrays.asList("kubernetes://", "kubernetes-loadbalancers://", "kubernetes-ingress-lbs://"));
        STACKS_TO_CLEANUP_KIND.put(SystemStackUpdate.KUBERNETES,
                Arrays.asList("kubernetesStack"));
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
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        List<Stack> toCleanup = new ArrayList<>();
        List<String> stackExternalIdPrefixes = STACKS_TO_CLEANUP_EXTERNAL_ID.get(systemStackType);
        for (Stack stack : all) {
            boolean removeByExternalId = false;
            if (stack.getExternalId() != null && stackExternalIdPrefixes != null) {
                for (String prefix : stackExternalIdPrefixes) {
                    if (stack.getExternalId().startsWith(prefix)) {
                        toCleanup.add(stack);
                        removeByExternalId = true;
                        break;
                    }
                }
                if (removeByExternalId) {
                    continue;
                }
                if (STACKS_TO_CLEANUP_KIND.get(systemStackType) != null
                        && STACKS_TO_CLEANUP_KIND.get(systemStackType).contains(stack.getKind())) {
                    toCleanup.add(stack);
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
