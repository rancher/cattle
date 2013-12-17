package io.github.ibuildthecloud.api.manager.subscribe;

import java.util.Map;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractBaseResourceManager;

public class SubscribeManager extends AbstractBaseResourceManager {

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return Subscribe.class;
    }

    @Override
    protected Object listInternal(String type, Map<Object, Object> criteria, ListOptions options) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

}
