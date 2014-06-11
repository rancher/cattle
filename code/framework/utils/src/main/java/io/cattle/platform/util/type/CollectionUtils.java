package io.cattle.platform.util.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {

    public static <T> Set<T> set(T... objects) {
        Set<T> set = new HashSet<T>();
        for ( T obj : objects) {
            set.add(obj);
        }

        return set;
    }

    public static Object get(Object map, String... keys) {
        Object value = map;
        for ( String key : keys ) {
            Map<String,Object> mapObject = CollectionUtils.toMap(value);
            value = mapObject.get(key);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> void set(Map<T, Object> map, Object value, T... keys) {
        for ( int i = 0 ; i < keys.length ; i++ ) {
            T key = keys[i];

            if ( key == null ) {
                return;
            }

            if ( i == keys.length - 1 ) {
                map.put(keys[i], value);
            } else {
                Map<T, Object> nestedMap = (Map<T, Object>)map.get(keys[i]);
                if ( nestedMap == null ) {
                    nestedMap = new HashMap<T, Object>();
                    map.put(key, nestedMap);
                }
                map = nestedMap;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K,V extends Collection<T>,T> void addToMap(Map<K,V> data, K key, T value, Class<? extends Collection> clz) {
        V values = data.get(key);
        if ( values == null ) {
            try {
                values = (V)clz.newInstance();
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Failed to create collection class", e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to create collection class", e);
            }

            data.put(key, values);
        }

        values.add(value);
    }
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
    public static <T> Map<T,Object> asMap(T key, Object... values) {
        Map<T,Object> result = new LinkedHashMap<T, Object>();

        if ( values == null || values.length % 2 == 0 ) {
            throw new IllegalArgumentException("value[] must be not null and an odd length");
        }

        result.put(key, values[0]);
        for ( int i = 1 ; i < values.length ; i+=2 ) {
            result.put((T)values[i], values[i+1]);
        }

        return result;
    }
}
