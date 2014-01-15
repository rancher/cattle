package io.github.ibuildthecloud.dstack.eventing.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.PoolSpecificListener;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

public class MethodInvokingListener implements EventListener, PoolSpecificListener {

    Method method;
    Object target;
    EventHandler handler;

    public MethodInvokingListener(EventHandler handler, Method method, Object target) {
        super();
        this.method = method;
        this.target = target;
        this.handler = handler;
    }

    @Override
    public void onEvent(Event event) {
        try {
            method.invoke(target, event);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        } catch (InvocationTargetException e) {
            ExceptionUtils.rethrowRuntime(e.getCause());
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        }
    }

    @Override
    public boolean isAllowQueueing() {
        if ( target instanceof PoolSpecificListener ) {
            return ((PoolSpecificListener)target).isAllowQueueing();
        }

        return handler.allowQueueing();
    }

    @Override
    public int getQueueDepth() {
        if ( target instanceof PoolSpecificListener ) {
            return ((PoolSpecificListener)target).getQueueDepth();
        }

        return handler.queueDepth();
    }

    @Override
    public String getPoolKey() {
        if ( target instanceof PoolSpecificListener ) {;
            return ((PoolSpecificListener)target).getPoolKey();
        }

        return handler.poolKey();
    }

}
