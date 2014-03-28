package io.cattle.platform.core.util;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;

public class ConstantsUtils {

    public static String property(Class<?> clz, String name) {
        try {
            PropertyUtils.getPropertyDescriptor(clz, name);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }

        return name;
    }
}
