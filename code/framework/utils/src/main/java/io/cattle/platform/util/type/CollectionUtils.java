package io.cattle.platform.util.type;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

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
                    nestedMap = new HashMap<T, Object>();
                    map.put(key, nestedMap);
                }
                map = nestedMap;
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V extends Collection<T>, T> void addToMap(Map<K, V> data, K key, T value, Class<? extends Collection> clz) {
        V values = data.get(key);
        if (values == null) {
            try {
                values = (V) clz.newInstance();
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
        if (obj instanceof List) {
            return (List<?>) obj;
        } else if (obj == null) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(obj);
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Object obj) {
        if (obj == null) {
            return new HashMap<K, V>();
        }

        if (obj instanceof Map) {
            return (Map<K, V>) obj;
        } else {
            return new HashMap<K, V>();
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> castMap(Object obj) {
        if (obj == null) {
            return new HashMap<K, V>();
        }

        if (obj instanceof Map) {
            return (Map<K, V>) obj;
        } else {
            throw new IllegalArgumentException("Expected [" + obj + "] to be a Map");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Map<T, Object> asMap(T key, Object... values) {
        Map<T, Object> result = new LinkedHashMap<T, Object>();

        if (values == null || values.length % 2 == 0) {
            throw new IllegalArgumentException("value[] must be not null and an odd length");
        }

        result.put(key, values[0]);
        for (int i = 1; i < values.length; i += 2) {
            result.put((T) values[i], values[i + 1]);
        }

        return result;
    }

    private static List<?> getObjectsByName(Map<String, Object> objectByName, String name) {
        Object obj = objectByName.get(name);
        if (obj == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(obj);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> List<T> orderList(Class<?> clz, List<T> objects) {
        String key = NamedUtils.toDotSeparated(clz.getSimpleName());
        Map<String, Object> objectsByName = new HashMap<>();
        final Map<Object, String> objectToName = new HashMap<>();

        if (objects != null) {
            for (T obj : objects) {
                String name = NamedUtils.getName(obj);
                objectsByName.put(name, obj);
                objectToName.put(obj, name);
            }
        }

        Set<String> excludes = getSetting(key + ".exclude");

        String list = ArchaiusUtil.getString(key + ".list").get();
        if (!StringUtils.isBlank(list)) {
            List<Object> result = new ArrayList<Object>();
            for (String name : list.split("\\s*,\\s*")) {
                if (excludes.contains(name)) {
                    continue;
                }

                result.addAll(getObjectsByName(objectsByName, name));
            }
            return (List<T>) result;
        }

        Set<String> includes = getSetting(key + ".include");

        Set<Object> ordered = new TreeSet<Object>(new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                int left = PriorityUtils.getPriority(o1);
                int right = PriorityUtils.getPriority(o2);
                if (left < right) {
                    return -1;
                } else if (left > right) {
                    return 1;
                }
                String leftName = objectToName.get(o1);
                String rightName = objectToName.get(o2);
                int comparisonResult = leftName.compareTo(rightName);
                if (comparisonResult == 0 && !o1.equals(o2)) {
                    throw new RuntimeException("Trying to add 2 objects with the same name: " + leftName + ".  Second object is ignored!");
                }
                return comparisonResult;
            }
        });

        if (objects != null) {
            ordered.addAll(objects);
        }

        for (String include : includes) {
            ordered.addAll(getObjectsByName(objectsByName, include));
        }

        Iterator<Object> iter = ordered.iterator();
        while (iter.hasNext()) {
            String name = objectToName.get(iter.next());
            if (excludes.contains(name)) {
                iter.remove();
            }
        }

        return (List<T>) new ArrayList<Object>(ordered);
    }

    private static Set<String> getSetting(String key) {
        String value = getSettingValue(key);
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<String>();

        for (String part : value.trim().split("\\s*,\\s*")) {
            result.add(part);
        }

        return result;
    }

    private static String getSettingValue(String key) {
        return ArchaiusUtil.getString(key).get();
    }

}
