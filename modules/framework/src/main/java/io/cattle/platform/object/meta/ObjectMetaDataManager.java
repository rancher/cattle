package io.cattle.platform.object.meta;

import io.cattle.platform.eventing.model.Event;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.util.Map;

public interface ObjectMetaDataManager {

    String TRANSITIONING_YES = Event.TRANSITIONING_YES;
    String TRANSITIONING_NO = Event.TRANSITIONING_NO;
    String TRANSITIONING_ERROR = Event.TRANSITIONING_ERROR;
    String TRANSITIONING_ERROR_OVERRIDE = Event.TRANSITIONING_ERROR + "Override";

    String TRANSITIONING_FIELD = "transitioning";
    String TRANSITIONING_MESSAGE_FIELD = "transitioningMessage";
    String TRANSITIONING_MESSAGE_DEFAULT_FIELD = "In Progress";

    String STATE_FIELD = "state";
    String STATES_FIELD = "states";

    String ACCOUNT_FIELD = "accountId";
    String CAPABILITIES_FIELD = "capabilities";
    String DATA_FIELD = "data";
    String KIND_FIELD = "kind";
    String ID_FIELD = TypeUtils.ID_FIELD;
    String NAME_FIELD = "name";
    String PUBLIC_FIELD = "isPublic";
    String REMOVED_FIELD = "removed";
    String REMOVE_TIME_FIELD = "removeTime";
    String TYPE_FIELD = "type";
    String UUID_FIELD = "uuid";
    String CREATED_FIELD = "created";
    String SYSTEM_FIELD = "system";

    String APPEND = "+";

    String convertToPropertyNameString(Class<?> recordClass, Object key);

    Object convertFieldNameFor(String type, Object key);

    Map<String, String> getLinks(SchemaFactory schemaFactory, String type);

    Map<String, Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type);

    Relationship getRelationship(String type, String linkName);

    Relationship getRelationship(Class<?> clz, String linkName);

    Map<String, Object> getTransitionFields(Schema schema, Object obj);

    Map<String, ActionDefinition> getActionDefinitions(Object obj);

    boolean isTransitioningState(Class<?> resourceType, String state);
}
