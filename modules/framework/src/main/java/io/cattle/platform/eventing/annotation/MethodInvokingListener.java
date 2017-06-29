package io.cattle.platform.eventing.annotation;

import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.eventing.EventListener;
import io.cattle.platform.eventing.PoolSpecificListener;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.util.exception.ExceptionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodInvokingListener implements EventListener, PoolSpecificListener {

    private static final Logger log = LoggerFactory.getLogger(MethodInvokingListener.class);

    LockManager lockManager;
    JsonMapper jsonMapper;
    Method method;
    Object target;
    EventHandler handler;
    boolean marshall = false;
    Class<?> targetType = null;
    Constructor<? extends LockDefinition> ctor = null;

    public MethodInvokingListener(LockManager lockManager, JsonMapper jsonMapper, EventHandler handler, Method method, Object target) {
        super();
        this.lockManager = lockManager;
        this.jsonMapper = jsonMapper;
        this.method = method;
        this.target = target;
        this.handler = handler;

        if (method.getParameterTypes().length > 1) {
            throw new IllegalArgumentException("Illegal EventHandler method, must have 0 or 1 arguments [" + method + "]");
        } else if (method.getParameterTypes().length == 1) {
            targetType = method.getParameterTypes()[0];
            if (targetType != Event.class) {
                marshall = true;
            }
        }

        if (handler.lock() != LockDefinition.class) {
            try {
                ctor = handler.lock().getConstructor(Event.class);
            } catch (SecurityException e) {
                throw new IllegalStateException("Failed to get constructor with Event.class for [" + handler.lock() + "]");
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to get constructor with Event.class for [" + handler.lock() + "]");
            }
        }
    }

    @Override
    public void onEvent(final Event event) {
        final Object arg;
        if (marshall) {
            arg = jsonMapper.convertValue(event, targetType);
        } else {
            arg = event;
        }

        try {
            lockManager.lock(getLock(event), new LockCallbackNoReturn() {
                @Override
                public void doWithLockNoResult() {
                    try {
                        method.invoke(target, arg);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
                    } catch (InvocationTargetException e) {
                        ExceptionUtils.rethrowRuntime(e.getCause());
                        throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
                    }
                }
            });
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        } catch (InvocationTargetException e) {
            ExceptionUtils.rethrowRuntime(e.getCause());
            throw new IllegalStateException("Failed to invoke method [" + method + "] for event [" + event + "", e);
        } catch (TimeoutException | FailedToAcquireLockException e) {
            log.info(e.getMessage() + " for event [" + event + "]");
        }
    }

    protected LockDefinition getLock(Event event) throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return ctor == null ? null : ctor.newInstance(event);
    }

    @Override
    public String getPoolKey() {
        if (target instanceof PoolSpecificListener) {
            ;
            return ((PoolSpecificListener) target).getPoolKey();
        }

        return handler.poolKey();
    }

}
