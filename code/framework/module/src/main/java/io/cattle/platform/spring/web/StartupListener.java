package io.cattle.platform.spring.web;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.NamedUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.netflix.config.DynamicStringListProperty;

public class StartupListener implements ServletContextListener {

    private static DynamicStringListProperty START_ORDER = ArchaiusUtil.getList("startup.priority");
    private static final Logger log = LoggerFactory.getLogger("ConsoleStatus");

    @Inject
    List<InitializationTask> tasks;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        WebApplicationContextUtils
            .getRequiredWebApplicationContext(event.getServletContext())
            .getAutowireCapableBeanFactory()
            .autowireBean(this);

        TreeSet<InitializationTask> sorted = new TreeSet<>(new Comparator<InitializationTask>() {
            @Override
            public int compare(InitializationTask o1, InitializationTask o2) {
                String left = NamedUtils.getName(o1);
                if (left == null) {
                    throw new IllegalStateException("Name is null for " + o1);
                }
                int result = left.compareTo(NamedUtils.getName(o2));
                if (result == 0) {
                    return Integer.compare(o1.hashCode(), o2.hashCode());
                }
                return result;
            }
        });

        List<InitializationTask> runOrder = new ArrayList<>();

        Map<String, InitializationTask> byName = new HashMap<>();
        for (InitializationTask task : tasks) {
            byName.put(NamedUtils.getName(task), task);
        }

        sorted.addAll(tasks);

        for (String name : START_ORDER.get()) {
            InitializationTask task = byName.get(name);
            if (task == null) {
                continue;
            }

            runOrder.add(task);
            sorted.remove(task);
        }

        try {
            Class<?> schemaFactory = Class.forName("io.github.ibuildthecloud.gdapi.factory.SchemaFactory");
            Iterator<InitializationTask> taskIter = sorted.iterator();
            while (taskIter.hasNext()) {
                InitializationTask task = taskIter.next();
                if (schemaFactory.isAssignableFrom(task.getClass())) {
                    runOrder.add(task);
                    taskIter.remove();
                }
            }
        } catch (ClassNotFoundException e) {
        }

        runOrder.addAll(sorted);

        for (int i = 0; i < runOrder.size() ; i++) {
            InitializationTask task = runOrder.get(i);
            log.info("Starting [{}/{}]: {}", i+1, runOrder.size(), NamedUtils.getName(task));
            task.start();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

    }


}
