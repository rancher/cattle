package io.github.ibuildthecloud.dstack.engine.handler;

import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HandlerResult {

    boolean shouldContinue = false;
    boolean shouldDelegate = false;
    Map<Object, Object> data;
    Map<String, Object> notes;

    public HandlerResult() {
        this(false, null);
    }

    public HandlerResult(Object key, Object... values) {
        this(false, CollectionUtils.asMap(key, values));
    }

    @SuppressWarnings("unchecked")
    public HandlerResult(Map<?, Object> data) {
        this(false, (Map<Object, Object>)data);
    }

    public HandlerResult(boolean shouldContinue, Map<Object, Object> data) {
        super();
        this.shouldContinue = shouldContinue;
        this.data = Collections.unmodifiableMap(data == null ? new HashMap<Object,Object>() : data);
    }

    public boolean shouldContinue() {
        return shouldContinue;
    }

    public Map<Object, Object> getData() {
        return data;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + (shouldContinue ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HandlerResult other = (HandlerResult) obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (shouldContinue != other.shouldContinue)
            return false;
        return true;
    }

    public boolean shouldDelegate() {
        return shouldDelegate;
    }

    public void shouldDelegate(boolean shouldDelegate) {
        this.shouldDelegate = shouldDelegate;
    }

}
