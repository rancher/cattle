package io.cattle.platform.task.eventing.impl;

import io.cattle.platform.eventing.lock.EventLock;
import io.cattle.platform.framework.event.ExecuteTask;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.task.TaskManager;
import io.cattle.platform.task.eventing.TaskManagerEventListener;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskManagerEventListenerImpl implements TaskManagerEventListener {

    private static final Logger log = LoggerFactory.getLogger(TaskManagerEventListenerImpl.class);

    TaskManager taskManager;
    LockManager lockManager;

    @Override
    public void executeTask(ExecuteTask event) {
        final String name = event.getData().getName();
        final Runnable runnable = taskManager.getRunnable(name);

        if (runnable != null) {
            lockManager.lock(getLock(event), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    log.info("Running task [{}]", name);
                    runnable.run();
                }
            });
        }
    }

    protected LockDefinition getLock(ExecuteTask event) {
        if (taskManager.shouldLock(event.getName())) {
            return new EventLock(event);
        } else {
            return null;
        }
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    @Inject
    public void setTaskManager(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}
