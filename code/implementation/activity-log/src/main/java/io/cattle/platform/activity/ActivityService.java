package io.cattle.platform.activity;

import io.cattle.platform.activity.impl.ActivityLogImpl;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.object.ObjectManager;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class ActivityService {

    private static ManagedThreadLocal<ActivityLog> TL = new ManagedThreadLocal<>();

    ObjectManager objectManager;
    EventService eventService;

    public ActivityService(ObjectManager objectManager, EventService eventService) {
        this.objectManager = objectManager;
        this.eventService = eventService;
    }

    private ActivityLog newLog(Long accountId) {
        ActivityLog log = TL.get();
        if (log == null) {
            log = new ActivityLogImpl(objectManager, eventService, accountId);
            TL.set(log);
        }
        return log;
    }

    public void info(String message, Object... args) {
        ActivityLog activityLog = TL.get();
        if (activityLog == null) {
            return;
        }
        activityLog.info(message, args);
    }

    public void error(String message, Object... args) {
        ActivityLog activityLog = TL.get();
        if (activityLog == null) {
            return;
        }
        activityLog.error(message, args);
    }

    public void waiting() {
        ActivityLog activityLog = TL.get();
        if (activityLog == null) {
            return;
        }
        activityLog.waiting();
    }

    public void run(Long accountId, Long serviceId, Long deploymentUnitId, String type, String message, Runnable run) {
        ActivityLog log = newLog(accountId);
        try (Entry entry = log.start(serviceId, deploymentUnitId, type, message)) {
            try {
                run.run();
            } catch (RuntimeException | Error e) {
                if (!(e instanceof ProcessDelayException) && !(e instanceof FailedToAcquireLockException)) {
                    entry.exception(e);
                } else if (e instanceof ProcessDelayException) {
                    log.waiting();
                }
                throw e;
            }
        }
    }

    public void instance(Long instanceId, String operation, String reason, String level) {
        ActivityLog activityLog = TL.get();
        if (activityLog == null) {
            return;
        }
        activityLog.instance(instanceId, operation, reason, level);
    }
}
