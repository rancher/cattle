package io.github.ibuildthecloud.dstack.api.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public class ApiUtils {

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
}
