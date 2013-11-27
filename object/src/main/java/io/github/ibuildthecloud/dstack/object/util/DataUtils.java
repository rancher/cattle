package io.github.ibuildthecloud.dstack.object.util;

import io.github.ibuildthecloud.dstack.util.type.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtils;

public class DataUtils {

    public static final String DATA = "data";
    public static final String OPTIONS = "options";
    public static final String FIELDS = "fields";

    @SuppressWarnings("unchecked")
    public static <T> List<T> getFieldList(Map<String,Object> data, String name, Class<T> type) {
        Map<String,Object> fields = MapUtils.castMap(data.get(FIELDS));
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

    @SuppressWarnings("unchecked")
    public static <T> T getField(Map<String,Object> data, String name, Class<T> type) {
        if ( data == null ) {
            return null;
        }

        Map<String,Object> fields = MapUtils.castMap(data.get(FIELDS));
        Object value = fields.get(name);

        if ( value == null ) {
            return null;
        }

        return (T)ConvertUtils.convert(value, type);
    }
}
