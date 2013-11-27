package io.github.ibuildthecloud.dstack.engine.handler;

import java.util.HashMap;
import java.util.Map;

public class HandlerResult {

    public static final HandlerResult EMPTY_RESULT = new HandlerResult();

    boolean shouldContinue = false;
    Map<Object, Object> data;

    public HandlerResult() {
        this(false, null);
    }

    public HandlerResult(Map<Object, Object> data) {
        this(false, data);
    }

    public HandlerResult(boolean shouldContinue, Map<Object, Object> data) {
        super();
        this.shouldContinue = shouldContinue;
        this.data = data == null ? new HashMap<Object,Object>() : data;
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

}
