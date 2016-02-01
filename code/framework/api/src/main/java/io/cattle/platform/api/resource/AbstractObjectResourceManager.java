package io.cattle.platform.api.resource;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.link.LinkHandler;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.manager.ProcessNotFoundException;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ActionDefinition;
import io.cattle.platform.object.meta.MapRelationship;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.NamedUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.id.IdentityFormatter;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractBaseResourceManager;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicIntProperty;

public abstract class AbstractObjectResourceManager extends AbstractBaseResourceManager implements InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(AbstractObjectResourceManager.class);

    public static final String SCHEDULE_UPDATE = "scheduleUpdate";

    private static final DynamicIntProperty REMOVE_DELAY = ArchaiusUtil.getInt("api.show.removed.for.seconds");
    private static final IdFormatter IDENTITY_FORMATTER = new IdentityFormatter();

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    ObjectMetaDataManager metaDataManager;
    Map<String, ActionHandler> actionHandlersMap;
    List<ActionHandler> actionHandlers;
    Map<String, List<LinkHandler>> linkHandlersMap;
    List<LinkHandler> linkHandlers;

    @Override
    protected Object authorize(Object object) {
        return ApiUtils.authorize(object);
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Class<?> clz = getClassForType(request.getSchemaFactory(), type);
        if (clz == null) {
            return null;
        }

        return doCreate(type, clz, CollectionUtils.toMap(request.getRequestObject()));
    }

    protected <T> T doCreate(String type, Class<T> clz, Map<Object, Object> data) {
        Map<String, Object> properties = getObjectManager().convertToPropertiesFor(clz, data);
        if (!properties.containsKey(ObjectMetaDataManager.KIND_FIELD)) {
            properties.put(ObjectMetaDataManager.KIND_FIELD, type);
        }

        return createAndScheduleObject(clz, properties);
    }

    @SuppressWarnings("unchecked")
    protected <T> T createAndScheduleObject(Class<T> clz, Map<String, Object> properties) {
        Object result = objectManager.create(clz, properties);
        try {
            scheduleProcess(StandardProcess.CREATE, result, properties);
            result = objectManager.reload(result);
        } catch (ProcessNotFoundException e) {
        }

        return (T) result;
    }

    protected Class<?> getClassForType(SchemaFactory schemaFactory, String type) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        if (clz == null) {
            Schema schema = schemaFactory.getSchema(type);
            if (schema != null && schema.getParent() != null) {
                return getClassForType(schemaFactory, schema.getParent());
            }
        }

        return clz;
    }

    @Override
    protected Object deleteInternal(String type, String id, Object obj, ApiRequest request) {
        try {
            scheduleProcess(StandardProcess.REMOVE, obj, null);
            return objectManager.reload(obj);
        } catch (ProcessCancelException e) {
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED);
        } catch (ProcessNotFoundException e) {
            return removeFromStore(type, id, obj, request);
        }
    }

    protected abstract Object removeFromStore(String type, String id, Object obj, ApiRequest request);

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        Map<String, Object> updates = CollectionUtils.toMap(request.getRequestObject());
        Map<String, Object> existingValues = new HashMap<>();
        Map<String, Object> filteredUpdates = new HashMap<String, Object>();
        Map<String, Object> existing = createResource(obj, IDENTITY_FORMATTER, request).getFields();
        Schema schema = request.getSchemaFactory().getSchema(type);
        Map<String, Field> fields = schema.getResourceFields();

        boolean schedule = false;
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object existingValue = existing.get(key);
            if (!ObjectUtils.equals(existingValue, entry.getValue())) {
                filteredUpdates.put(key, entry.getValue());
                existingValues.put(key, existingValue);
                Field field = fields.get(key);
                if (field != null) {
                    schedule |= Boolean.TRUE.equals(field.getAttributes().get(SCHEDULE_UPDATE));
                }
            }

        }

        Object result = objectManager.setFields(schema, obj, filteredUpdates);
        if (schedule) {
            filteredUpdates.put("old", existingValues);
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, obj, filteredUpdates);
            result = objectManager.reload(result);
        }

        return result;
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        List<LinkHandler> linkHandlers = linkHandlersMap.get(type);
        if (linkHandlers != null) {
            for (LinkHandler linkHandler : linkHandlers) {
                if (linkHandler.handles(type, id, link, request)) {
                    Object currentObject = getById(type, id, new ListOptions(request));
                    if (currentObject == null) {
                        return null;
                    }

                    try {
                        return linkHandler.link(link, currentObject, request);
                    } catch (IOException e) {
                        log.error("Failed to process link [{}] for [{}:{}]", link, type, id, e);
                        return null;
                    }
                }
            }
        }

        Class<?> clz = request.getSchemaFactory().getSchemaClass(type, true);
        Relationship relationship;
        if (clz != null) {
            relationship = getRelationship(clz, link);
        } else {
            relationship = getRelationship(type, link);
        }

        if (relationship == null) {
            return null;
        }

        switch (relationship.getRelationshipType()) {
        case CHILD:
            return getChildLink(type, id, relationship, request);
        case REFERENCE:
            return getReferenceLink(type, id, relationship, request);
        case MAP:
            return getMapLink(type, id, (MapRelationship) relationship, request);
        }

        return null;
    }

    protected Object getMapLink(String type, String id, MapRelationship relationship, ApiRequest request) {
        return null;
    }

    protected Object getChildLink(String type, String id, Relationship relationship, ApiRequest request) {
        Schema otherSchema = request.getSchemaFactory().getSchema(relationship.getObjectType());
        if (otherSchema == null) {
            return Collections.EMPTY_LIST;
        }

        Object currentObject = getById(type, id, new ListOptions(request));
        if (currentObject == null) {
            return null;
        }

        String otherType = otherSchema.getId();
        Field field = otherSchema.getResourceFields().get(relationship.getPropertyName());
        if (field == null) {
            return Collections.EMPTY_LIST;
        }

        if (otherSchema.getCollectionMethods().contains(Method.POST.toString())) {
            Map<String, Object> createDefaults = new HashMap<String, Object>();
            IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
            createDefaults.put(relationship.getPropertyName(), idFormatter.formatId(type, id));
            request.setCreateDefaults(createDefaults);
        }

        Map<Object, Object> criteria = getDefaultCriteria(false, true, otherType);
        criteria.put(relationship.getPropertyName(), id);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherType);
        return resourceManager.list(otherType, criteria, null);
    }

    protected Object getReferenceLink(String type, String id, Relationship relationship, ApiRequest request) {
        SchemaFactory schemaFactory = request.getSchemaFactory();
        Schema schema = schemaFactory.getSchema(type);
        Schema otherSchema = schemaFactory.getSchema(relationship.getObjectType());
        Field field = schema.getResourceFields().get(relationship.getPropertyName());
        if (field == null || otherSchema == null) {
            return null;
        }

        ListOptions options = new ListOptions(request);
        Object currentObject = getById(type, id, options);
        if (currentObject == null) {
            return null;
        }

        Object fieldValue = field.getValue(currentObject);

        if (fieldValue == null) {
            return null;
        }

        Map<Object, Object> criteria = getDefaultCriteria(false, true, otherSchema.getId());
        criteria.put(ObjectMetaDataManager.ID_FIELD, fieldValue);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherSchema.getId());
        return ApiUtils.getFirstFromList(resourceManager.list(otherSchema.getId(), criteria, options));
    }

    protected Map<String, Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type, Include include) {
        if (include == null)
            return Collections.emptyMap();

        Map<String, Relationship> result = new HashMap<String, Relationship>();
        Map<String, Relationship> links = metaDataManager.getLinkRelationships(schemaFactory, type);
        for (String link : include.getLinks()) {
            link = link.toLowerCase();
            if (links.containsKey(link)) {
                result.put(link, links.get(link));
            }
        }

        return result;
    }

    @Override
    protected Map<Object, Object> getDefaultCriteria(boolean byId, boolean byLink, String type) {
        Map<Object, Object> criteria = super.getDefaultCriteria(byId, byLink, type);
        Policy policy = ApiUtils.getPolicy();

        addAccountAuthorization(byId, byLink, type, criteria, policy);

        if (!policy.isOption(Policy.REMOVED_VISIBLE) && !byId) {
            /* removed is null or removed >= (NOW() - delay) */
            Condition or = new Condition(new Condition(ConditionType.NULL), new Condition(ConditionType.GTE, removedTime()));
            criteria.put(ObjectMetaDataManager.REMOVED_FIELD, or);

            /* remove_time is null or remove_time > NOW() */
            or = new Condition(new Condition(ConditionType.NULL), new Condition(ConditionType.GT, new Date()));
            criteria.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, or);
        }

        return criteria;
    }

    protected Date removedTime() {
        return new Date(System.currentTimeMillis() - REMOVE_DELAY.get() * 1000);
    }

    protected void addAccountAuthorization(boolean byId, boolean byLink, String type, Map<Object, Object> criteria, Policy policy) {
        if (!policy.isOption(Policy.LIST_ALL_ACCOUNTS)) {
            if (policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) && (byId || byLink)) {
                return;
            }

            criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, policy.getAccountId());
        }
    }

    @Override
    protected String getCollectionType(List<?> list, ApiRequest request) {
        String link = request.getLink();
        if (link == null) {
            return request.getType();
        } else {
            Relationship relationship = getRelationship(request.getSchemaFactory()
                    .getSchemaClass(request.getType(), true), link);
            return request.getSchemaFactory().getSchemaName(relationship.getObjectType());
        }
    }

    @Override
    protected Schema getSchemaForDisplay(SchemaFactory schemaFactory, Object obj) {
        String schemaId = ApiUtils.getSchemaIdForDisplay(schemaFactory, obj);
        Schema schema = schemaFactory.getSchema(schemaId);

        if (schema == null) {
            /* Check core schema because the parent might not be authorized */
            schemaId = ApiUtils.getSchemaIdForDisplay(getObjectManager().getSchemaFactory(), obj);

            /* Still get schema from request's schemaFactory */
            schema = schemaFactory.getSchema(schemaId);
        }

        return schema;
    }

    @Override
    protected Resource constructResource(IdFormatter idFormatter, SchemaFactory schemaFactory, Schema schema, Object obj, ApiRequest apiRequest) {
        Map<String, Object> transitioningFields = metaDataManager.getTransitionFields(schema, obj);
        return ApiUtils.createResourceWithAttachments(this, apiRequest, idFormatter, schema, obj, transitioningFields);
    }

    @Override
    protected Object resourceActionInternal(Object obj, ApiRequest request) {
        String processName = getProcessName(obj, request);
        ActionHandler handler = actionHandlersMap.get(processName);
        if (handler != null) {
            return handler.perform(processName, obj, request);
        }

        Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());

        try {
            scheduleProcess(getProcessName(obj, request), obj, data);
        } catch (ProcessNotFoundException e) {
            throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
        }

        request.setResponseCode(ResponseCodes.ACCEPTED);
        return objectManager.reload(obj);
    }

    protected String getProcessName(Object obj, ApiRequest request) {
        String baseType = request.getSchemaFactory().getBaseType(request.getType());
        return String.format("%s.%s", baseType == null ? request.getType() : baseType, request.getAction()).toLowerCase();
    }

    protected void scheduleProcess(final String processName, final Object resource, final Map<String, Object> data) {
        scheduleProcess(new Runnable() {
            @Override
            public void run() {
                objectProcessManager.scheduleProcessInstance(processName, resource, data);
            }
        });
    }

    protected void scheduleProcess(final StandardProcess process, final Object resource, final Map<String, Object> data) {
        scheduleProcess(new Runnable() {
            @Override
            public void run() {
                objectProcessManager.scheduleStandardProcess(process, resource, data);
            }
        });
    }

    protected void scheduleProcess(Runnable run) {
        try {
            run.run();
        } catch (ProcessInstanceException e) {
            if (e.getExitReason() == ExitReason.RESOURCE_BUSY || e.getExitReason() == ExitReason.CANCELED) {
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected void addActions(Object obj, SchemaFactory schemaFactory, Schema schema, Resource resource) {
        Object state = resource.getFields().get(ObjectMetaDataManager.STATE_FIELD);
        Map<String, ActionDefinition> defs = metaDataManager.getActionDefinitions(obj);

        if (state == null || defs == null) {
            super.addActions(obj, schemaFactory, schema, resource);
            return;
        }

        Map<String, Action> actions = schema.getResourceActions();

        if (actions == null || actions.size() == 0) {
            return;
        }

        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

        for (Map.Entry<String, Action> entry : actions.entrySet()) {
            String name = entry.getKey();
            Action action = entry.getValue();

            if (!isValidAction(obj, action)) {
                continue;
            }

            ActionDefinition def = defs.get(name);
            if (def == null || def.getValidStates().contains(state)) {
                resource.getActions().put(name, urlBuilder.actionLink(resource, name));
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected boolean isValidAction(Object obj, Action action) {
        Map<String, Object> attributes = action.getAttributes();

        if (attributes == null || attributes.size() == 0) {
            return true;
        }

        String capability = ObjectUtils.toString(attributes.get("capability"), null);
        String state = ObjectUtils.toString(attributes.get(ObjectMetaDataManager.STATE_FIELD), null);
        String currentState = io.cattle.platform.object.util.ObjectUtils.getState(obj);

        if (!StringUtils.isBlank(capability) && !(ApiContext.getContext().getCapabilities(obj) != null ?
                ApiContext.getContext().getCapabilities(obj).contains(capability) :
                DataAccessor.fieldStringList(obj, ObjectMetaDataManager.CAPABILITIES_FIELD).contains(capability))) {
            return false;
        }

        if (!StringUtils.isBlank(state) && !state.equals(currentState)) {
            return false;
        }
        List<String> states = ((List<String>) attributes.get(ObjectMetaDataManager.STATES_FIELD));
        if (states != null && !states.contains(currentState)){
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        actionHandlersMap = NamedUtils.createMapByName(actionHandlers);
        linkHandlersMap = new HashMap<String, List<LinkHandler>>();

        for (LinkHandler handler : linkHandlers) {
            for (String type : handler.getTypes()) {
                CollectionUtils.addToMap(linkHandlersMap, type, handler, ArrayList.class);
            }
        }
    }

    protected Relationship getRelationship(String type, String linkName) {
        return metaDataManager.getRelationship(type, linkName);
    }

    protected Relationship getRelationship(Class<?> clz, String linkName) {
        return metaDataManager.getRelationship(clz, linkName);
    }

    @Override
    public void stop() {
    }

    @Override
    protected Object collectionActionInternal(Object resources, ApiRequest request) {
        return null;
    }

    @Override
    protected Map<String, String> getLinks(SchemaFactory schemaFactory, Resource resource) {
        return metaDataManager.getLinks(schemaFactory, resource.getType());
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

    public ObjectProcessManager getObjectProcessManager() {
        return objectProcessManager;
    }

    @Inject
    public void setObjectProcessManager(ObjectProcessManager objectProcessManager) {
        this.objectProcessManager = objectProcessManager;
    }

    public List<ActionHandler> getActionHandlers() {
        return actionHandlers;
    }

    @Inject
    public void setActionHandlers(List<ActionHandler> actionHandlers) {
        this.actionHandlers = actionHandlers;
    }

    public List<LinkHandler> getLinkHandlers() {
        return linkHandlers;
    }

    @Inject
    public void setLinkHandlers(List<LinkHandler> linkHandlers) {
        this.linkHandlers = linkHandlers;
    }
}
