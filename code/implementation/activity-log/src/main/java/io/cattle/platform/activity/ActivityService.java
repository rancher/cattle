package io.cattle.platform.activity;

import io.cattle.platform.activity.impl.ActivityLogImpl;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class ActivityService {

    @Inject
    ObjectManager objectManager;
    @Inject
    EventService eventService;

    private static ManagedThreadLocal<ActivityLog> TL = new ManagedThreadLocal<ActivityLog>();

    public ActivityLog newLog() {
        ActivityLog log = TL.get();
        if (log == null) {
            log = new ActivityLogImpl(objectManager, eventService);
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

    public void run(Service service, String type, String message, Runnable run) {
        ActivityLog log = newLog();
        try (Entry entry = log.start(service, type, message)) {
            try {
                run.run();
            } catch (RuntimeException|Error e) {
                entry.exception(e);
                throw e;
            }
        }
    }

    public void instance(Instance instance, String operation, String reason, String level) {
        ActivityLog activityLog = TL.get();
        if (activityLog == null) {
            return;
        }
        activityLog.instance(instance, operation, reason, level);
    }
}
