package io.github.ibuildthecloud.dstack.iaas.config.impl;

import javax.inject.Inject;

import io.github.ibuildthecloud.dstack.iaas.config.ScopedConfig;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.dstack.server.context.ServerContext;

public class ScopedConfigImpl implements ScopedConfig {

    ObjectManager objectManager;

    @Override
    public String getConfigUrl(Object context) {
        return getUrl(context, CONFIG_URL);
    }

    @Override
    public String getStorageUrl(Object context) {
        return getUrl(context, STORAGE_URL);
    }

    @Override
    public String getApiUrl(Object context) {
        return getUrl(context, API_URL);
    }

    @Override
    public String getConfigUrl(Class<?> type, Object id) {
        return getUrl(type, id, CONFIG_URL);
    }

    @Override
    public String getStorageUrl(Class<?> type, Object id) {
        return getUrl(type, id, STORAGE_URL);
    }

    @Override
    public String getApiUrl(Class<?> type, Object id) {
        return getUrl(type, id, API_URL);
    }

    @Override
    public String getUrl(Object context, String name) {
        Object id = ObjectUtils.getId(context);

        return getUrl(context == null ? null : context.getClass(), id, name);
    }

    @Override
    public String getUrl(Class<?> typeClass, Object id, String name) {
        String type = objectManager.getType(typeClass);
        if ( type == null || id == null ) {
            return ServerContext.getServerAddress(name).getUrlString();
        } else {
            return ServerContext.getServerAddress(String.format("%s.%s", type, id), name).getUrlString();
        }
    }


    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
