package io.cattle.platform.extension.spring;

import io.cattle.platform.util.type.InitializationTask;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.SmartLifecycle;

public class BackgroundStart implements BeanPostProcessor, Runnable, SmartLifecycle {

    List<InitializationTask> tasks = new ArrayList<InitializationTask>();
    boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        for ( InitializationTask task : tasks ) {
            task.stop();
        }
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if ( InitializationTask.class.isAssignableFrom(bean.getClass()) ) {
            tasks.add((InitializationTask)bean);
        }

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void run() {
        for ( InitializationTask task : tasks ) {
            task.start();
        }
    }

    @Override
    public int getPhase() {
        return 1000;
    }

}
