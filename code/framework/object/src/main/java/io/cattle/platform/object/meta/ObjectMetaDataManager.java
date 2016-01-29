package io.cattle.platform.object.meta;

import io.cattle.platform.eventing.model.Event;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.Map;

public interface ObjectMetaDataManager {

    public static final String TRANSITIONING_YES = Event.TRANSITIONING_YES;
    public static final String TRANSITIONING_NO = Event.TRANSITIONING_NO;
    public static final String TRANSITIONING_ERROR = Event.TRANSITIONING_ERROR;

    public static final String TRANSITIONING_FIELD = "transitioning";
    public static final String TRANSITIONING_PROGRESS_FIELD = "transitioningProgress";
    public static final String TRANSITIONING_MESSAGE_FIELD = "transitioningMessage";
    public static final String TRANSITIONING_MESSAGE_DEFAULT_FIELD = "In Progress";

    public static final String STATE_FIELD = "state";
    public static final String STATES_FIELD = "states";

    public static final String ACCOUNT_FIELD = "accountId";
    public static final String CAPABILITIES_FIELD = "capabilities";
    public static final String DATA_FIELD = "data";
    public static final String KIND_FIELD = "kind";
    public static final String ID_FIELD = TypeUtils.ID_FIELD;
    public static final String NAME_FIELD = "name";
    public static final String PUBLIC_FIELD = "isPublic";
    public static final String REMOVED_FIELD = "removed";
    public static final String REMOVE_TIME_FIELD = "removeTime";
    public static final String TYPE_FIELD = "type";
    public static final String UUID_FIELD = "uuid";

    public static final String MAP_SUFFIX = "Map";

    public static final String APPEND = "+";

    String convertToPropertyNameString(Class<?> recordClass, Object key);

    String lookupPropertyNameFromFieldName(Class<?> recordClass, String fieldName);

    Object convertFieldNameFor(String type, Object key);

    Map<String, String> getLinks(SchemaFactory schemaFactory, String type);

    Map<String, Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type);

    Relationship getRelationship(String type, String linkName);

    Relationship getRelationship(Class<?> clz, String linkName);

    Relationship getRelationship(String type, String linkName, String fieldName);

    Relationship getRelationship(Class<?> clz, String linkName, String fieldName);

    Map<String, Object> getTransitionFields(Schema schema, Object obj);

    Map<String, ActionDefinition> getActionDefinitions(Object obj);

    boolean isTransitioningState(Class<?> resourceType, String state);
}
