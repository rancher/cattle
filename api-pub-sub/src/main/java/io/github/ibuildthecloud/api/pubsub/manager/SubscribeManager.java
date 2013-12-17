package io.github.ibuildthecloud.api.pubsub.manager;

import java.util.Collections;
import java.util.Map;

import io.github.ibuildthecloud.api.pubsub.model.Subscribe;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

public class SubscribeManager extends AbstractNoOpResourceManager {

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Subscribe.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Subscribe subscribe = request.proxyRequestObject(Subscribe.class);

        return super.createInternal(type, request);
    }

    @Override
    protected Object listInternal(String type, Map<Object, Object> criteria, ListOptions options) {
        return Collections.EMPTY_LIST;
    }

}
