package io.cattle.platform.activity;

import io.cattle.platform.activity.impl.ActivityLogImpl;
import io.cattle.platform.object.ObjectManager;

import javax.inject.Inject;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivityService {
    
    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

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

    public void run(Object operator, String type, Runnable run) {
        ActivityLog log = newLog();
        try (Entry entry = log.start(operator, type)) {
            try {
                run.run();
            } catch (RuntimeException|Error e) {
                entry.exception(e);
            }
        }
    }

}
