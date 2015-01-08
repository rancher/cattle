package io.cattle.platform.engine.server.impl;

import javax.inject.Inject;

import io.cattle.platform.engine.server.ProcessServer;
import io.cattle.platform.engine.server.lock.ProcessReplayLock;
import io.cattle.platform.lock.LockDelegator;
import io.cattle.platform.task.Task;

public class ProcessReplayTask implements Task {

    ProcessServer processServer;
    LockDelegator lockDelegator;

    @Override
    public void run() {
        if (lockDelegator.tryLock(new ProcessReplayLock())) {
            processServer.runOutstandingJobs();
        }
    }

    @Override
    public String getName() {
        return "process.replay";
    }

    public LockDelegator getLockDelegator() {
        return lockDelegator;
    }

    @Inject
    public void setLockDelegator(LockDelegator lockDelegator) {
        this.lockDelegator = lockDelegator;
    }

    public ProcessServer getProcessServer() {
        return processServer;
    }

    @Inject
    public void setProcessServer(ProcessServer processServer) {
        this.processServer = processServer;
    }

}
