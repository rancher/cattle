package io.cattle.platform.core.dao;

import java.util.List;

public interface GenericMapDao {

    <T> T findNonRemoved(Class<T> mapType,
            Class<?> leftResourceType, long leftResourceId,
            Class<?> rightResourceType, long rightResourceId);

    <T> List<? extends T> findNonRemoved(Class<T> mapType, Class<?> resourceType, long resourceId);

    <T> List<? extends T> findToRemove(Class<T> mapType, Class<?> resourceType, long resourceId);

    <T> T findToRemove(Class<T> mapType, Class<?> leftResourceType, long leftResourceId, Class<?> rightResourceType,
            long rightResourceId);

    <T> List<? extends T> findAll(Class<T> mapType, Class<?> resourceType, long resourceId);
}
