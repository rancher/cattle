package io.github.ibuildthecloud.dstack.util.type;

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

    public static final String getName(Object obj) {
        if ( obj instanceof Named ) {
            return ((Named)obj).getName();
        } else {
            return obj.getClass().getSimpleName();
        }
    }

}
