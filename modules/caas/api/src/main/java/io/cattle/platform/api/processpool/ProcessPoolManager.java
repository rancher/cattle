package io.cattle.platform.api.processpool;

import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.core.addon.ProcessPool;
import io.cattle.platform.util.type.NamedUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class ProcessPoolManager extends AbstractNoOpResourceManager {

    List<ThreadPoolExecutor> executors;

    public ProcessPoolManager(ThreadPoolExecutor... executors) {
        super();
        this.executors = Arrays.asList(executors);
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        List<ProcessPool> pools = new ArrayList<>();

        for (ThreadPoolExecutor executor : executors) {
            ProcessPool pool = new ProcessPool();
            pool.setName(NamedUtils.getName(executor));
            pool.setActiveTasks(executor.getActiveCount());
            pool.setPoolSize(executor.getPoolSize());
            pool.setMinPoolSize(executor.getCorePoolSize());
            pool.setMaxPoolSize(executor.getMaximumPoolSize());
            pool.setCompletedTasks(executor.getCompletedTaskCount());
            pool.setQueueSize(executor.getQueue().size());
            pool.setQueueRemainingCapacity(executor.getQueue().remainingCapacity());
            try {
                Object obj = PropertyUtils.getProperty(executor, "rejectedExecutionCount");
                if (obj instanceof Number) {
                    pool.setRejectedTasks(((Number) obj).longValue());
                }
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            }

            pools.add(pool);
        }

        return pools;
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        return null;
    }

}