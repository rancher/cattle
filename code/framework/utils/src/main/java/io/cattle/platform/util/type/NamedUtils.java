package io.cattle.platform.util.type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedUtils {

    public static <T extends Named> Map<String, T> createMapByName(List<T> items) {
        Map<String, T> result = new HashMap<String, T>();

        for ( T item : items ) {
            result.put(item.getName(), item);
        }

        return result;
    }

    public static String getName(Object obj) {
        if ( obj instanceof Named ) {
            return ((Named)obj).getName();
        } else {
            return obj.getClass().getSimpleName();
        }
    }

    public static String toDotSeparated(String name) {
        if ( name == null ) {
            return name;
        }
        return name.replaceAll("([a-z])([A-Z])", "$1.$2").toLowerCase();
    }
}
