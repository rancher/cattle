package io.cattle.platform.core.dao;

import java.util.Map;

public interface GenericResourceDao {

    <T> T createAndSchedule(Class<T> clz, Map<String, Object> resource);

    <T> T createAndSchedule(Class<T> clz, Object key, Object... values);

    <T> T createAndSchedule(T obj, Map<String, Object> processData);

    <T> T create(Class<T> clz, Map<String, Object> resource);

    <T> T createAndSchedule(T o);

    <T> T updateAndSchedule(T o);

    <T> T updateAndSchedule(T o, Map<String, Object> fields);

}
