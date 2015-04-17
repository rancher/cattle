package io.cattle.platform.core.dao;

import io.cattle.platform.core.model.ProjectMember;

import java.util.Map;

public interface GenericResourceDao {

    <T> T createAndSchedule(Class<T> clz, Map<String, Object> resource);

    <T> T createAndSchedule(Class<T> clz, Object key, Object... values);

    <T> T create(Class<T> clz, Map<String, Object> resource);
}
