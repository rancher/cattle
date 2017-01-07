package io.cattle.platform.spring.resource;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.util.type.Named;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;

import fr.xebia.springframework.concurrent.ThreadPoolExecutorFactory.SpringJmxEnabledThreadPoolExecutor;

public class SpringConfigurableExecutorService extends SpringJmxEnabledThreadPoolExecutor implements Named {

    String name;

    public SpringConfigurableExecutorService(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler rejectedExecutionHandler,
            ObjectName objectName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler, objectName);
        this.name = name;
    }

    public static SpringConfigurableExecutorService byName(String name) throws MalformedObjectNameException {
        return byName(name, new ThreadPoolExecutor.AbortPolicy());
    }

    public static SpringConfigurableExecutorService byName(String name, RejectedExecutionHandler rejectedExecutionHandler) throws MalformedObjectNameException {
        final DynamicIntProperty corePoolSize = ArchaiusUtil.getInt("pool." + name.toLowerCase() + ".core.size");
        final DynamicIntProperty maxPoolSize = ArchaiusUtil.getInt("pool." + name.toLowerCase() + ".max.size");
        final DynamicIntProperty keepAliveTime = ArchaiusUtil.getInt("pool." + name.toLowerCase() + ".keep.alive");
        DynamicIntProperty queueSize = ArchaiusUtil.getInt("pool." + name.toLowerCase() + ".queue.size");
        DynamicBooleanProperty priorityQueue = ArchaiusUtil.getBoolean("pool." + name.toLowerCase() + ".priority.queue");

        BlockingQueue<Runnable> workQueue = new SynchronousQueue<>();
        int size = queueSize.get();
        if (size > 0) {
            if (priorityQueue.get()) {
                workQueue = new BoundedQueue<>(size);
            } else {
                workQueue = new LinkedBlockingQueue<>(size);

            }
        } else if (size < 0) {
            workQueue = new LinkedBlockingQueue<>();

        }

        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory(name + "-");
        threadFactory.setDaemon(true);

        final SpringConfigurableExecutorService es =
                new SpringConfigurableExecutorService(name, corePoolSize.get(), maxPoolSize.get(),
                        keepAliveTime.get(), TimeUnit.SECONDS, workQueue, threadFactory, rejectedExecutionHandler,
                        new ObjectName("java.util.concurrent:type=ThreadPoolExecutor,name=" + name));

        corePoolSize.addCallback(new Runnable() {
            @Override
            public void run() {
                es.setCorePoolSize(corePoolSize.get());
            }
        });
        maxPoolSize.addCallback(new Runnable() {
            @Override
            public void run() {
                es.setMaximumPoolSize(maxPoolSize.get());
            }
        });
        keepAliveTime.addCallback(new Runnable() {
            @Override
            public void run() {
                es.setKeepAliveTime(keepAliveTime.get(), TimeUnit.SECONDS);
            }
        });

        return es;
    }

    public static class BoundedQueue<E> extends PriorityBlockingQueue<E> {
        private static final long serialVersionUID = 8385439019119907779L;

        int capacity;

        public BoundedQueue(int capacity) {
            super();
            this.capacity = capacity;
        }

        @Override
        public boolean offer(E e) {
            // Fuzzy, but good enough
            if (size() > capacity) {
                return false;
            }
            return super.offer(e);
        }

        @Override
        public int remainingCapacity() {
            return capacity - size();
        }
    }

    @Override
    public String getName() {
        return name;
    }

}
