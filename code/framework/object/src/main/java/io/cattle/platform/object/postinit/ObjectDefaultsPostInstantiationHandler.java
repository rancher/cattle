package io.cattle.platform.object.postinit;

import io.cattle.platform.object.ObjectDefaultsProvider;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.Priority;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectDefaultsPostInstantiationHandler implements ObjectPostInstantiationHandler, InitializationTask, Priority {

    private static final Logger log = LoggerFactory.getLogger(ObjectDefaultsPostInstantiationHandler.class);

    List<ObjectDefaultsProvider> defaultProviders;
    Map<Class<?>,Map<String,Object>> defaults = new HashMap<Class<?>, Map<String,Object>>();

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String,Object> properties) {
        try {
            applyDefaults(clz, obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to apply defaults to [" + obj + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to apply defaults to [" + obj + "]", e);
        }
        return obj;
    }

    protected void applyDefaults(Class<?> clz, Object instance) throws IllegalAccessException, InvocationTargetException {
        Map<String,Object> defaultValues = defaults.get(instance.getClass());

        if ( defaultValues == null ) {
            return;
        }

        log.debug("Applying defaults [{}] to [{}]", defaultValues, instance);
        BeanUtils.copyProperties(instance, defaultValues);
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    @Override
    public void start() {
        for ( ObjectDefaultsProvider provider : defaultProviders ) {
            defaults.putAll(provider.getDefaults());
        }
    }

    @Override
    public void stop() {
    }

    public List<ObjectDefaultsProvider> getDefaultProviders() {
        return defaultProviders;
    }

    @Inject
    public void setDefaultProviders(List<ObjectDefaultsProvider> defaultProviders) {
        this.defaultProviders = defaultProviders;
    }


}
