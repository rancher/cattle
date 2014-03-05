package io.github.ibuildthecloud.dstack.core.dao;

import java.util.Map;

public interface GenericResourceDao {

    <T> T createAndSchedule(Class<T> clz, Map<String, Object> resource);

}
