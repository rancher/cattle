package io.github.ibuildthecloud.dstack.object.meta;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.Map;

public interface ObjectMetaDataManager {

    public static final String TRANSITIONING_YES = "yes";
    public static final String TRANSITIONING_NO = "no";
    public static final String TRANSITIONING_ERROR = "error";

    public static final String TRANSITIONING_FIELD = "transitioning";
    public static final String TRANSITIONING_PROGRESS_FIELD = "transitioningProgress";
    public static final String TRANSITIONING_MESSAGE_FIELD = "transitioningMessage";
    public static final String TRANSITIONING_MESSAGE_DEFAULT_FIELD = "In Progress";

    public static final String KIND_FIELD = "kind";
    public static final String STATE_FIELD = "state";
    public static final String ACCOUNT_FIELD = "accountId";
    public static final String REMOVE_TIME_FIELD = "removeTime";
    public static final String ID_FIELD = TypeUtils.ID_FIELD;

    String convertToPropertyNameString(Class<?> recordClass, Object key);

    String lookupPropertyNameFromFieldName(Class<?> recordClass, String fieldName);

    Object convertFieldNameFor(String type, Object key);

    Map<String,String> getLinks(SchemaFactory schemaFactory, String type);

    Map<String,Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type);

    Relationship getRelationship(String type, String linkName);

    Map<String,Object> getTransitionFields(Schema schema, Object obj);
}
