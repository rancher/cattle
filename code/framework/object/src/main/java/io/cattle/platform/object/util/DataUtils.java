package io.cattle.platform.object.util;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;

public class DataUtils {

    public static final String DATA = "data";
    public static final String OPTIONS = "options";
    public static final String FIELDS = "fields";

    public static String getState(Object obj) {
        try {
            return BeanUtils.getProperty(obj, ObjectMetaDataManager.STATE_FIELD);
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }

        return null;
    }

    public static Map<String,Object> getFields(Object obj) {
        Map<String,Object> data = DataAccessor.getData(obj, true);
        Map<String,Object> fields = CollectionUtils.toMap(data.get(FIELDS));
        return Collections.unmodifiableMap(fields);
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> getWritableFields(Object obj) {
        Map<String,Object> data = DataAccessor.getData(obj, false);
        Map<String,Object> fields = (Map<String, Object>)data.get(FIELDS);

        if ( fields == null ) {
            fields = new HashMap<String, Object>();
            data.put(FIELDS, fields);
        }

        return fields;
    }

    protected static void setData(Object obj, Map<String,Object> data) {
        ObjectUtils.setPropertyIgnoreErrors(obj, DATA, data);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getFieldList(Map<String,Object> data, String name, Class<T> type) {
        Map<String,Object> fields = CollectionUtils.castMap(data.get(FIELDS));
        Object value = fields.get(name);

        if ( value == null ) {
            return null;
        }

        if ( value instanceof List ) {
            List<?> list = (List<?>)value;
            if ( list.size() > 0 && type.isAssignableFrom(list.get(0).getClass()) ) {
                return (List<T>) list;
            }

            List<T> result = new ArrayList<T>(list.size());
            for ( Object obj : list ) {
                result.add((T)ConvertUtils.convert(obj, type));
            }
            return result;
        } else {
            throw new IllegalArgumentException("[" + value + "] is not a list");
        }
    }

    //@SuppressWarnings("unchecked")
    //public static <T> T getField(Map<String,Object> data, String name, Class<T> type) {
        //Map<String,Object> fields = CollectionUtils.castMap(data.get(FIELDS));
        //Object value = fields.get(name);
//
        //if ( value == null ) {
            //return null;
        //}
//
        //return (T)ConvertUtils.convert(value, type);
    //}

    @SuppressWarnings("unchecked")
    public static <T> T getFieldFromRequest(ApiRequest request, String name, Class<T> type) {
        if ( request == null ) {
            return null;
        }

        Map<String,Object> fields = CollectionUtils.castMap(request.getRequestObject());
        Object value = fields.get(name);

        if ( value == null ) {
            return null;
        }

        return (T)ConvertUtils.convert(value, type);
    }
}
