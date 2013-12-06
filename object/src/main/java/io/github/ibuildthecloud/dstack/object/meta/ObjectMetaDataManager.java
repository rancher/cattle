package io.github.ibuildthecloud.dstack.object.meta;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.Map;

public interface ObjectMetaDataManager {

    public static final String ACCOUNT_FIELD = "accountId";
    public static final String REMOVE_TIME_FIELD = "removeTime";
    public static final String ID_FIELD = TypeUtils.ID_FIELD;

    String convertToPropertyNameString(Class<?> recordClass, Object key);

    String lookupPropertyNameFromFieldName(Class<?> recordClass, String fieldName);

    Object convertFieldNameFor(String type, Object key);

    Map<String,String> getLinks(SchemaFactory schemaFactory, String type);

    Map<String,Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type);

    Relationship getRelationship(String type, String linkName);
}
