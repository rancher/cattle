package io.cattle.platform.activity;

import io.cattle.platform.activity.impl.ActivityLogImpl;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public class ActivityService {
    
    @Inject
    ObjectManager objectManager;
    
    private static ManagedThreadLocal<ActivityLog> TL = new ManagedThreadLocal<ActivityLog>();

    public ActivityLog newLog() {
        ActivityLog log = TL.get();
        if (log == null) {
            log = new ActivityLogImpl(objectManager);
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

    public void run(Object operator, String type, String message, Runnable run) {
        ActivityLog log = newLog();
        try (Entry entry = log.start(operator, type, message)) {
            try {
                run.run();
            } catch (RuntimeException|Error e) {
                entry.exception(e);
                throw e;
            }
        }
    }

    public void instance(Instance instance, String operation, String reason) {
        ActivityLog activityLog = TL.get();
        if (activityLog == null) {
            return;
        }
        activityLog.instance(instance, operation, reason);
    }
}
