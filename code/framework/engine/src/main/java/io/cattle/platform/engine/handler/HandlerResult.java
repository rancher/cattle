package io.cattle.platform.engine.handler;

import io.cattle.platform.engine.process.ProcessPhase;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HandlerResult {

    Boolean shouldContinue;
    String chainProcessName;
    Map<Object, Object> data;

    public HandlerResult() {
        this((Boolean) null, (Map<Object, Object>) null);
    }

    public HandlerResult(Object key, Object... values) {
        this(null, CollectionUtils.asMap(key, values));
    }

    @SuppressWarnings("unchecked")
    public HandlerResult(Map<?, Object> data) {
        this(null, (Map<Object, Object>) data);
    }

    public HandlerResult(Boolean shouldContinue, Map<Object, Object> data) {
        super();
        this.shouldContinue = shouldContinue;
        this.data = Collections.unmodifiableMap(data == null ? new HashMap<>() : data);
    }

    public Boolean shouldContinue(ProcessPhase phase) {
        if (shouldContinue == null) {
            return phase != ProcessPhase.HANDLERS;
        }
        return shouldContinue;
    }

    public Map<Object, Object> getData() {
        return data;
    }

    public HandlerResult withShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
        return this;
    }

    public HandlerResult withChainProcessName(String processName) {
        this.chainProcessName = processName;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((shouldContinue == null) ? 0 : shouldContinue.hashCode());
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
        if (shouldContinue == null) {
            if (other.shouldContinue != null)
                return false;
        } else if (!shouldContinue.equals(other.shouldContinue))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "HandlerResult [shouldContinue=" + shouldContinue + ", data=" + data + "]";
    }

    public String getChainProcessName() {
        return chainProcessName;
    }

    public void setChainProcessName(String chainProcessName) {
        this.chainProcessName = chainProcessName;
    }

}
