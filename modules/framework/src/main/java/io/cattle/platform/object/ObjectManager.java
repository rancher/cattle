package io.cattle.platform.object;

import io.cattle.platform.object.meta.Relationship;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.List;
import java.util.Map;

public interface ObjectManager {

    <T> T newRecord(Class<T> type);

    <T> T create(T obj);

    <T> T create(T obj, Map<String, Object> properties);

    <T> T create(T obj, Object key, Object... valueKeyValue);

    <T> T create(Class<T> clz, Map<String, Object> properties);

    <T> T create(Class<T> clz, Object key, Object... valueKeyValue);

    <T> T reload(T obj);

    <T> T persist(T obj);

    void delete(Object obj);

    <T> T loadResource(Class<T> type, String resourceId);

    <T> T loadResource(Class<T> type, Long resourceId);

    <T> T loadResource(String resourceType, String resourceId);

    <T> T loadResource(String resourceType, Long resourceId);

    <T> T setFields(T obj, Map<String, Object> values);

    <T> T setFields(Schema schema, T obj, Map<String, Object> values);

    <T> T setFields(T obj, Object key, Object... valueKeyValue);

    Map<String, Object> convertToPropertiesFor(Object obj, Map<Object, Object> object);

    <T> List<T> children(Object obj, Class<T> type);

    <T> List<T> children(Object obj, Class<T> type, String propertyName);

    <T> T findOne(Class<T> clz, Map<Object, Object> values);

    <T> T findOne(Class<T> clz, Object key, Object... valueKeyValue);

    <T> T findAny(Class<T> clz, Map<Object, Object> values);

    <T> T findAny(Class<T> clz, Object key, Object... valueKeyValue);

    <T> List<T> find(Class<T> clz, Map<Object, Object> values);

    <T> List<T> find(Class<T> clz, Object key, Object... valueKeyValue);

    <T> List<T> getListByRelationship(Object obj, Relationship relationship);

    String getType(Object obj);

    SchemaFactory getSchemaFactory();

}
