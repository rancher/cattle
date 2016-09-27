package io.cattle.platform.iaas.api.filter.common;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class CachedOutputFilter<T> implements ResourceOutputFilter {

    protected abstract T newObject(ApiRequest apiRequest);

    protected abstract Long getId(Object obj);

    protected T getCached(ApiRequest apiRequest) {
        if (apiRequest == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        T cached = (T)apiRequest.getAttribute(this);
        if (cached == null) {
            cached = newObject(apiRequest);
            apiRequest.setAttribute(this, cached);
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