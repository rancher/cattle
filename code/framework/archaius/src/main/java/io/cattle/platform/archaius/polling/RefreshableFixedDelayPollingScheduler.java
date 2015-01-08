package io.cattle.platform.archaius.polling;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.FixedDelayPollingScheduler;

public class RefreshableFixedDelayPollingScheduler extends FixedDelayPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshableFixedDelayPollingScheduler.class);

    List<Runnable> tasks = new ArrayList<Runnable>();

    public RefreshableFixedDelayPollingScheduler() {
        super();
    }

    public RefreshableFixedDelayPollingScheduler(int initialDelayMillis, int delayMillis, boolean ignoreDeletesFromSource) {
        super(initialDelayMillis, delayMillis, ignoreDeletesFromSource);
    }

    @Override
    protected synchronized void schedule(Runnable runnable) {
        tasks.add(runnable);
        super.schedule(runnable);
    }

    public void refresh() {
        for (Runnable runnable : tasks) {
            try {
                runnable.run();
            } catch (Throwable t) {
                log.error("Failed to reload configuration", t);
            }
        }
    }
}
