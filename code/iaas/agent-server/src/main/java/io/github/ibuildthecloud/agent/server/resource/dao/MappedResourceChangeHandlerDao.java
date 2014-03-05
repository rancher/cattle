package io.github.ibuildthecloud.agent.server.resource.dao;

import io.github.ibuildthecloud.agent.server.resource.impl.MissingDependencyException;

import java.util.Map;
import java.util.Set;

public interface MappedResourceChangeHandlerDao {

    Set<String> getMappedUuids(String uuid, Class<?> resourceType, Class<?> mappingType, Class<?> otherType);

    <T> T newResource(Class<T> resourceType, Class<?> mappingType, Class<?> otherType,
            Map<String,Object> properties, String mappedUuid) throws MissingDependencyException;

}
