package io.cattle.platform.object.postinit;

import io.cattle.platform.object.ObjectDefaultsProvider;
import io.cattle.platform.object.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectDefaultsPostInstantiationHandler implements ObjectPostInstantiationHandler {

    private static final Logger log = LoggerFactory.getLogger(ObjectDefaultsPostInstantiationHandler.class);

    List<ObjectDefaultsProvider> defaultProviders;
    Map<Class<?>, Map<String, Object>> defaults = new HashMap<>();

    public ObjectDefaultsPostInstantiationHandler(ObjectDefaultsProvider... defaultProviders) {
        super();
        this.defaultProviders = Arrays.asList(defaultProviders);
    }

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
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
        Map<String, Object> defaultValues = defaults.get(instance.getClass());

        if (defaultValues == null) {
            return;
        }

        log.debug("Applying defaults [{}] to [{}]", defaultValues, ObjectUtils.toStringWrapper(instance));
        BeanUtils.copyProperties(instance, defaultValues);
    }

    public void start() {
        for (ObjectDefaultsProvider provider : defaultProviders) {
            defaults.putAll(provider.getDefaults());
        }
    }

    public List<ObjectDefaultsProvider> getDefaultProviders() {
        return defaultProviders;
    }

}
