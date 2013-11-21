package io.github.ibuildthecloud.dstack.object;

import java.util.Map;

public interface ObjectManager {

    <T> T create(Class<T> clz, Map<String,Object> properties);

    <T> T reload(T obj);

    <T> T persist(T obj);

    <T> T loadResource(String resourceType, String resourceId);

    <T> T loadResource(String resourceType, Long resourceId);

    <T> T setFields(Object obj, Map<Object,Object> values);

    <T> T setFields(Object obj, Object key, Object... valueKeyValue);

}
