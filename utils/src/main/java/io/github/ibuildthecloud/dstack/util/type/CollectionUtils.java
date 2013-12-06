package io.github.ibuildthecloud.dstack.util.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtils {

    public static List<?> toList(Object obj) {
        if ( obj instanceof List ) {
            return (List<?>)obj;
        } else if ( obj == null ) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(obj);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> toMap(Object obj) {
        if ( obj == null ) {
            return new HashMap<K, V>();
        }

        if ( obj instanceof Map ) {
            return (Map<K, V>) obj;
        } else {
            return new HashMap<K, V>();
        }
    }

    @SuppressWarnings("unchecked")
    public static <K,V> Map<K,V> castMap(Object obj) {
        if ( obj == null ) {
            return new HashMap<K, V>();
        }

        if ( obj instanceof Map ) {
            return (Map<K, V>) obj;
        } else {
            throw new IllegalArgumentException("Expected [" + obj + "] to be a Map");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<T,Object> asMap(T key, Object... value) {
        Map<T,Object> result = new LinkedHashMap<T, Object>();

        if ( value == null || value.length % 2 == 0 ) {
            throw new IllegalArgumentException("value[] must be not null and an odd length");
        }

        result.put(key, value[0]);
        for ( int i = 1 ; i < value.length ; i+=2 ) {
            result.put((T)value[i], value[i+1]);
        }

        return result;
    }
}
