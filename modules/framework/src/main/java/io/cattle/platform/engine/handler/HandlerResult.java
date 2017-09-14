package io.cattle.platform.engine.handler;

import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HandlerResult {

    String chainProcessName;
    Map<Object, Object> data;
    ListenableFuture<?> future;

    public HandlerResult() {
        this((Map<?, Object>)null);
    }

    public HandlerResult(ListenableFuture<?> future) {
        this((Map<?, Object>)null);
        withFuture(future);
    }

    public HandlerResult(Object key, Object... values) {
        this(CollectionUtils.asMap(key, values));
    }

    public HandlerResult(Map<?, Object> data) {
        super();
        this.data = Collections.unmodifiableMap(data == null ? new HashMap<>() : data);
    }

    public HandlerResult(Boolean ignored, Map<Object, Object> data) {
        super();
        this.data = Collections.unmodifiableMap(data == null ? new HashMap<>() : data);
    }

    public Map<Object, Object> getData() {
        return data;
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
        return true;
    }

    @Override
    public String toString() {
        return "HandlerResult [data=" + data + "]";
    }

    public String getChainProcessName() {
        return chainProcessName;
    }

    public void setChainProcessName(String chainProcessName) {
        this.chainProcessName = chainProcessName;
    }

    public ListenableFuture<?> getFuture() {
        return future;
    }

    public void setFuture(ListenableFuture<?> future) {
        this.future = future;
    }

    public HandlerResult withFuture(ListenableFuture<?> future) {
        this.future = future;
        return this;
    }

}
