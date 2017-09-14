package io.cattle.platform.object.postinit;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class UUIDPostInstantiationHandler implements ObjectPostInstantiationHandler {

    public static final String UUID = "uuid";

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        set(obj, UUID, java.util.UUID.randomUUID().toString());
        return obj;
    }

    protected void set(Object obj, String property, Object value) {
        try {
            if (BeanUtils.getProperty(obj, property) == null) {
                BeanUtils.setProperty(obj, property, value);
            }
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
    }

}
