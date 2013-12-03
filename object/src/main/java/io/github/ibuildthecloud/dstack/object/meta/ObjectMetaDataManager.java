package io.github.ibuildthecloud.dstack.object.meta;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;

import java.util.Set;

public interface ObjectMetaDataManager {

    public static final String ACCOUNT_FIELD = "accountId";
    public static final String REMOVE_TIME_FIELD = "removeTime";
    public static final String ID_FIELD = "id";

    String convertPropertyNameFor(Class<?> recordClass, Object key);

    Object convertFieldNameFor(String type, Object key);

    Set<String> getLinks(SchemaFactory schemaFactory, Resource resource);

    Relationship getRelationship(String type, String linkName);
}
