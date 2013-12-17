package io.github.ibuildthecloud.dstack.object;

import java.util.List;
import java.util.Map;

public interface ObjectManager {

    <T> T create(Class<T> clz, Map<String,Object> properties);

    <T> T create(Class<T> clz, Object key, Object... valueKeyValue);

    <T> T reload(T obj);

    <T> T persist(T obj);

    <T> T loadResource(Class<T> type, String resourceId);

    <T> T loadResource(Class<T> type, Long resourceId);

    <T> T loadResource(String resourceType, String resourceId);

    <T> T loadResource(String resourceType, Long resourceId);

    <T> T setFields(Object obj, Map<String,Object> values);

    <T> T setFields(Object obj, Object key, Object... valueKeyValue);

    Map<String,Object> convertToPropertiesFor(Object obj, Map<Object,Object> object);

    <T> List<T> children(Object obj, Class<T> type);

}
