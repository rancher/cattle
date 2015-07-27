package io.github.ibuildthecloud.gdapi.request.resource.impl;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.Map;

public abstract class AbstractNoOpResourceManager extends AbstractBaseResourceManager {

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return null;
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        return null;
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        return null;
    }

    @Override
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        return null;
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        return null;
    }

    @Override
    protected Object resourceActionInternal(Object obj, ApiRequest request) {
        return null;
    }

    @Override
    protected Object collectionActionInternal(Object resources, ApiRequest request) {
        return null;
    }

}