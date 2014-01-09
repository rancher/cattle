package io.github.ibuildthecloud.dstack.object.util;

import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

public class ObjectUtils {

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

    public static Object getId(Object obj) {
        return getPropertyIgnoreErrors(obj, ObjectMetaDataManager.ID_FIELD);
    }

    public static String getKind(Object obj) {
        Object kind = getPropertyIgnoreErrors(obj, ObjectMetaDataManager.KIND_FIELD);
        return kind == null ? null : kind.toString();
    }

    public static Object getPropertyIgnoreErrors(Object obj, String property) {
        try {
            if ( obj == null ) {
                return null;
            }
            return PropertyUtils.getProperty(obj, property);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }

        return null;
    }


}
