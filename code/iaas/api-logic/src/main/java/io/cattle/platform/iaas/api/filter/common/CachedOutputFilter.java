package io.cattle.platform.iaas.api.filter.common;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.managed.threadlocal.ManagedThreadLocal;

public abstract class CachedOutputFilter<T> implements ResourceOutputFilter {

    private ManagedThreadLocal<T> cache = new ManagedThreadLocal<>();

    protected abstract T newObject(ApiRequest apiRequest);

    protected abstract Long getId(Object obj);

    protected T getCached(ApiRequest apiRequest) {
        T cached = cache.get();
        if (cached == null) {
            cached = newObject(apiRequest);
            cache.set(cached);
        }
        return cached;
    }

    protected List<Long> getIds(ApiRequest apiRequest) {
        if (apiRequest == null) {
            return Collections.emptyList();
        }

        Object responseObj = apiRequest.getResponseObject();
        if (responseObj instanceof List<?>) {
            List<Long> ret = new ArrayList<>(((List<?>) responseObj).size());
            for (Object obj : ((List<?>) responseObj)) {
                Long id = getId(obj);
                if (id != null) {
                    ret.add(id);
                }
            }
            return ret;
        } else {
            Long id = getId(responseObj);
            if (id != null) {
                return Arrays.asList(id);
            }
        }

        return Collections.emptyList();
    }

}