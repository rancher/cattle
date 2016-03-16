package io.cattle.platform.task.impl;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.framework.event.ExecuteTask;
import io.cattle.platform.task.Task;
import io.cattle.platform.task.TaskManager;
import io.cattle.platform.task.TaskOptions;
import io.cattle.platform.task.dao.TaskDao;
import io.cattle.platform.util.type.InitializationTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicStringProperty;

public class TaskManagerImpl implements TaskManager, InitializationTask, Runnable {

    private static final DynamicLongProperty DELAY_SECONDS = ArchaiusUtil.getLong("task.initial.delay.seconds");
    private static final String SCHEDULE_FORMAT = "task.%s.schedule";
    private static final Logger log = LoggerFactory.getLogger(TaskManagerImpl.class);

    ScheduledExecutorService executorService;
    List<Task> tasks;
    EventService eventService;
    boolean running = false;
    Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<String, ScheduledFuture<?>>();
    Map<String, Runnable> runnables = new ConcurrentHashMap<String, Runnable>();
    Map<String, Task> taskMap = new HashMap<String, Task>();
    TaskDao taskDao;

    @Override
    public void execute(String name) {
        DeferredUtils.deferPublish(eventService, new ExecuteTask(name));
    }

    @Override
    public Runnable getRunnable(String name) {
        return runnables.get(name);
    }

    @Override
    public boolean shouldLock(String name) {
        Task task = taskMap.get(name);
        if (task instanceof TaskOptions) {
            return ((TaskOptions) task).isShouldLock();
        }

        return true;
    }

    @Override
    public void start() {
        scheduleAll(true);
    }

    @Override
    public void run() {
        scheduleAll(false);
    }

    protected void scheduleAll(boolean initial) {
        for (Task task : tasks) {
            schedule(initial, task);
        }
    }

    protected void schedule(boolean initial, final Task task) {
        final String name = task.getName();
        ScheduledFuture<?> future = futures.get(name);

        if (future != null) {
            future.cancel(false);
        }

        DynamicStringProperty prop = ArchaiusUtil.getString(String.format(SCHEDULE_FORMAT, name));
        if (initial) {
            prop.addCallback(this);
        }

        try {
            Runnable runnable = new NoExceptionRunnable() {
                @Override
                protected void doRun() throws Exception {
                    try {
                        task.run();
                    } catch (Throwable t) {
                        log.error("Task [{}] failed", name, t);
                    }
                }
            };
            taskDao.register(name);
            runnables.put(name, runnable);
            taskMap.put(name, task);

            if (!StringUtils.isBlank(prop.get())) {
                long delay = (long) (Float.parseFloat(prop.get()) * 1000);
                if (delay == 0) {
                    log.info("Disabling task [{}]", name);
                    return;
                }

                log.info("Scheduling task [{}] for every [{}] seconds", name, delay);
                future = executorService.scheduleWithFixedDelay(runnable, Math.min(DELAY_SECONDS.get() * 1000, delay), delay, TimeUnit.MILLISECONDS);
                futures.put(name, future);
            }
        } catch (NumberFormatException nfe) {
            log.error("Failed to parse [{}] for task [{}]", prop.get(), name, nfe);
        }
    }

    @Override
    public void stop() {
        for (ScheduledFuture<?> future : futures.values()) {
            future.cancel(false);
        }
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    @Inject
    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public EventService getEventService() {
        return eventService;
    }

    @Inject
    public void setEventService(EventService eventService) {
        this.eventService = eventService;
    }

    public TaskDao getTaskDao() {
        return taskDao;
    }

    @Inject
    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    @Inject
    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

}
