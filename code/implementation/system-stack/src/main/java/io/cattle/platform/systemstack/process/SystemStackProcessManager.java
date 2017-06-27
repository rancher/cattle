package io.cattle.platform.systemstack.process;

import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.systemstack.listener.SystemStackUpdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemStackProcessManager {

    private static final Map<String, List<String>> STACKS_TO_CLEANUP_EXTERNAL_ID = new HashMap<>();
    private static final Map<String, List<String>> STACKS_TO_CLEANUP_KIND = new HashMap<>();
    static {
        STACKS_TO_CLEANUP_EXTERNAL_ID.put(AccountConstants.ORC_KUBERNETES,
                Arrays.asList("kubernetes://", "kubernetes-loadbalancers://", "kubernetes-ingress-lbs://"));
        STACKS_TO_CLEANUP_KIND.put(AccountConstants.ORC_KUBERNETES,
                Arrays.asList("kubernetesStack"));
    }

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    SystemStackUpdate update;
    SystemStackTrigger systemStackTrigger;
    ResourceMonitor resourceMonitor;
    NetworkDao networkDao;

    public SystemStackProcessManager(ObjectManager objectManager, ObjectProcessManager processManager, SystemStackUpdate update,
            SystemStackTrigger systemStackTrigger, ResourceMonitor resourceMonitor, NetworkDao networkDao) {
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.update = update;
        this.systemStackTrigger = systemStackTrigger;
        this.resourceMonitor = resourceMonitor;
        this.networkDao = networkDao;
    }

    public HandlerResult accountCreate(ProcessState state, ProcessInstance process) {
        final Account account = (Account)state.getResource();
        DeferredUtils.defer(new Runnable() {
            @Override
            public void run() {
                try {
                    update.createStacks(account);
                } catch (IOException e) {
                }
            }
        });
        return null;
    }

    public HandlerResult stackRemove(ProcessState state, ProcessInstance process) {
        Stack systemStack = (Stack) state.getResource();
        if (systemStack.getExternalId() == null) {
            return null;
        }

        String systemStackType = SystemStackUpdate.getStackTypeFromExternalId(systemStack.getExternalId());
        if (systemStackType == null) {
            return null;
        }

        for (Stack toCleanup : getStacksToCleanup(systemStack, systemStackType)) {
            processManager.scheduleStandardProcess(StandardProcess.REMOVE, toCleanup, null);
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

}
