package io.cattle.platform.object.lifecycle;

import java.util.Map;

public class AbstractObjectLifeCycleHandler implements ObjectLifeCycleHandler {

    @Override
    public <T> T onEvent(LifeCycleEvent event, T instance, Class<T> clz, Map<String, Object> properties) {
        switch (event) {
        case CREATE:
            return onCreate(instance, clz, properties);
        default:
            return instance;
        }
    }

    protected <T> T onCreate(T instance, Class<T> clz, Map<String, Object> properties) {
        return instance;
    }
}
