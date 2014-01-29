package io.github.ibuildthecloud.dstack.task.eventing.impl;

import io.github.ibuildthecloud.dstack.eventing.lock.EventLock;
import io.github.ibuildthecloud.dstack.framework.event.ExecuteTask;
import io.github.ibuildthecloud.dstack.lock.LockCallbackNoReturn;
import io.github.ibuildthecloud.dstack.lock.LockManager;
import io.github.ibuildthecloud.dstack.task.TaskManager;
import io.github.ibuildthecloud.dstack.task.eventing.TaskManagerEventListener;

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

        if ( runnable != null ) {
            lockManager.lock(new EventLock(event), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    log.info("Running task [{}]", name);
                    runnable.run();
                }
            });
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
