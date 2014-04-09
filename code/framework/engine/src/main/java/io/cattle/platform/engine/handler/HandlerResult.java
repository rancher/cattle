package io.cattle.platform.engine.handler;

import io.cattle.platform.engine.process.ProcessPhase;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HandlerResult {

    Boolean shouldContinue;
    boolean shouldDelegate = false;
    Map<Object, Object> data;

    public HandlerResult() {
        this((Boolean)null, (Map<Object, Object>)null);
    }

    public HandlerResult(Object key, Object... values) {
        this(null, CollectionUtils.asMap(key, values));
    }

    @SuppressWarnings("unchecked")
    public HandlerResult(Map<?, Object> data) {
        this(null, (Map<Object, Object>)data);
    }

    public HandlerResult(Boolean shouldContinue, Map<Object, Object> data) {
        super();
        this.shouldContinue = shouldContinue;
        this.data = Collections.unmodifiableMap(data == null ? new HashMap<Object,Object>() : data);
    }

    public Boolean shouldContinue(ProcessPhase phase) {
        if ( shouldContinue == null ) {
            return phase != ProcessPhase.HANDLERS;
        }
        return shouldContinue;
    }

    public Map<Object, Object> getData() {
        return data;
    }


    public boolean shouldDelegate() {
        return shouldDelegate;
    }

    public void shouldDelegate(boolean shouldDelegate) {
        this.shouldDelegate = shouldDelegate;
    }

    public HandlerResult withShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((shouldContinue == null) ? 0 : shouldContinue.hashCode());
        result = prime * result + (shouldDelegate ? 1231 : 1237);
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
        HandlerResult other = (HandlerResult)obj;
        if (data == null) {
            if (other.data != null)
                return false;
        } else if (!data.equals(other.data))
            return false;
        if (shouldContinue == null) {
            if (other.shouldContinue != null)
                return false;
        } else if (!shouldContinue.equals(other.shouldContinue))
            return false;
        if (shouldDelegate != other.shouldDelegate)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HandlerResult [shouldContinue=" + shouldContinue + ", shouldDelegate=" + shouldDelegate + ", data="
                + data + "]";
    }

}
