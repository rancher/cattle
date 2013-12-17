package io.github.ibuildthecloud.dstack.eventing.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.github.ibuildthecloud.dstack.eventing.EventListener;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.util.exception.ExceptionUtils;

public class MethodInvokingListener implements EventListener {

    Method method;
    Object target;

    public MethodInvokingListener(Method method, Object target) {
        super();
        this.method = method;
        this.target = target;
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

}
