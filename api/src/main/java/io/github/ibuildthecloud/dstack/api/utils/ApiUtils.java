package io.github.ibuildthecloud.dstack.api.utils;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.auth.impl.DefaultPolicy;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Collection;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class ApiUtils {

    private static final Policy DEFAULT_POLICY = new DefaultPolicy();

    @SuppressWarnings("unchecked")
    public static Map<String,Object> getMap(Object obj) {
        if ( obj instanceof Map ) {
            return (Map<String,Object>)obj;
        } else {
            return new HashMap<String, Object>();
        }
    }

    public static void copy(Object src, Object dest) {
        try {
            BeanUtils.copyProperties(dest, src);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to copy properties from [" + src + "] to ["
                    + dest + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to copy properties from [" + src + "] to ["
                    + dest + "]", e);
        }
    }

    public static Object getFirstFromList(Object obj) {
        if ( obj instanceof Collection ) {
            return getFirstFromList(((Collection)obj).getData());
        }

        if ( obj instanceof List ) {
            List<?> list = (List<?>)obj;
            return list.size() > 0 ? list.get(0) : null;
        }

        return null;
    }

    public static Policy getPolicy() {
        Object policy = ApiContext.getContext().getPolicy();
        if ( policy instanceof Policy ) {
            return (Policy)policy;
        } else {
            return DEFAULT_POLICY;
        }
    }

    public static <T> List<T> authorize(List<T> list) {
        return getPolicy().authorize(list);
    }

    @SuppressWarnings("unchecked")
    public static <T> T authorize(T obj) {
        if ( obj instanceof List ) {
            return (T) authorize((List<T>)obj);
        }
        return getPolicy().authorize(obj);
    }
}
