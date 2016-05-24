package io.cattle.platform.systemstack.listener;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import io.cattle.platform.core.model.Environment;
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
        STACKS_TO_CLEANUP.put("kubernetes",
                Arrays.asList("kubernetes://", "kubernetes-loadbalancers://", "kubernetes-ingress-lbs://"));
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { "environment.remove" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Environment systemStack = (Environment) state.getResource();
        if (systemStack.getExternalId() == null) {
            return null;
        }
        if (!systemStack.getExternalId().startsWith(String.format(SystemStackUpdate.STACK_EXTERNAL_ID, ""))) {
            return null;
        }

        for (Environment toCleanup : getStacksToCleanup(systemStack)) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, toCleanup, null);
        }
        return null;
    }

    protected List<Environment> getStacksToCleanup(Environment systemStack) {
        List<Environment> all = objectManager.find(Environment.class, ENVIRONMENT.ACCOUNT_ID,
                systemStack.getAccountId(),
                ENVIRONMENT.REMOVED, null);
        List<Environment> toCleanup = new ArrayList<>();
        String systemStackType = systemStack.getExternalId().replace(String.format(SystemStackUpdate.STACK_EXTERNAL_ID, ""), "");
        for (String prefix : STACKS_TO_CLEANUP.get(systemStackType)) {
            Iterator<Environment> it = all.iterator();
            while (it.hasNext()) {
                Environment stack = it.next();
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
        return toCleanup;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
