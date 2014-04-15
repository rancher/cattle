package io.cattle.platform.core.dao;

import java.util.Map;

public interface GenericResourceDao {

    <T> T createAndSchedule(Class<T> clz, Map<String, Object> resource);

    <T> T createAndSchedule(Class<T> clz, Object key, Object... values);

}
