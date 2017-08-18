package io.cattle.platform.util.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionUtils {

    public static Object getNestedValue(Object map, String... keys) {
        Object value = map;
        for (String key : keys) {
            Map<String, Object> mapObject = CollectionUtils.toMap(value);
            value = mapObject.get(key);
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> void setNestedValue(Map<T, Object> map, Object value, T... keys) {
        for (int i = 0; i < keys.length; i++) {
            T key = keys[i];

            if (key == null) {
                return;
            }

            if (i == keys.length - 1) {
                map.put(keys[i], value);
            } else {
                Map<T, Object> nestedMap = (Map<T, Object>) map.get(keys[i]);
                if (nestedMap == null) {
                    nestedMap = new HashMap<>();
                    map.put(key, nestedMap);
                }
                map = nestedMap;
            }
        }
    }

    public static List<?> toList(Object obj) {
        if (obj instanceof List) {
            return (List<?>) obj;
        } else if (obj == null) {
            return Collections.emptyList();
        } else {
            return Collections.singletonList(obj);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }

        if (obj instanceof Map) {
            return (Map<K, V>) obj;
        } else {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> castMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }

        if (obj instanceof Map) {
            return (Map<K, V>) obj;
        } else {
            throw new IllegalArgumentException("Expected [" + obj + "] to be a Map");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<T, Object> asMap(T key, Object... values) {
        if ((values == null || values.length == 0) && key instanceof Map) {
            return (Map<T, Object>) key;
        }

        Map<T, Object> result = new LinkedHashMap<>();

        if (values == null) {
            result.put(key, null);
            return result;
        }

        if (values.length % 2 == 0) {
            throw new IllegalArgumentException("value[] must be not null and an odd length");
        }

        result.put(key, values[0]);
        for (int i = 1; i < values.length; i += 2) {
            result.put((T) values[i], values[i + 1]);
        }

        return result;
    }

    @SafeVarargs
    public static <T> Set<T> set(T... object) {
        Set<T> result = new HashSet<>();
        for (T x : object) {
            result.add(x);
        }
        return result;
    }

}
