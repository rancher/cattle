package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.api.action.ActionHandler;
import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstanceException;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.NamedUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractBaseResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.netflix.config.DynamicIntProperty;

public abstract class AbstractObjectResourceManager extends AbstractBaseResourceManager implements InitializationTask {

//    private static final Logger log = LoggerFactory.getLogger(AbstractObjectResourceManager.class);

    private static final DynamicIntProperty REMOVE_DELAY = ArchaiusUtil.getInt("api.show.removed.for.seconds");

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    ObjectMetaDataManager metaDataManager;
    Map<String,ActionHandler> actionHandlersMap;
    List<ActionHandler> actionHandlers;

    @Override
    protected Object authorize(Object object) {
        return ApiUtils.authorize(object);
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Class<?> clz = getClassForType(request.getSchemaFactory(), type);
        if ( clz == null ) {
            return null;
        }

        return doCreate(type, clz, CollectionUtils.toMap(request.getRequestObject()));
    }

    @SuppressWarnings("unchecked")
    protected <T> T doCreate(String type, Class<T> clz, Map<Object,Object> data) {
        Map<String,Object> properties = getObjectManager().convertToPropertiesFor(clz, data);
        if ( ! properties.containsKey(ObjectMetaDataManager.KIND_FIELD) ) {
            properties.put(ObjectMetaDataManager.KIND_FIELD, type);
        }

        Object result = objectManager.create(clz, properties);
        try {
            scheduleProcess(StandardProcess.CREATE, result, properties);
            result = objectManager.reload(result);
        } catch ( ProcessNotFoundException e ) {
        }

        return (T)result;
    }

    protected Class<?> getClassForType(SchemaFactory schemaFactory, String type) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        if ( clz == null ) {
            Schema schema = schemaFactory.getSchema(type);
            if ( schema.getParent() != null ) {
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
        } catch ( ProcessNotFoundException e ) {
            return removeFromStore(type, id, obj, request);
        }
    }

    protected abstract Object removeFromStore(String type, String id, Object obj, ApiRequest request);

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        Map<String,Object> updates = CollectionUtils.toMap(request.getRequestObject());
        return objectManager.setFields(obj, updates);
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        Relationship relationship = metaDataManager.getRelationship(type, link);

        if ( relationship == null ) {
            return null;
        }

        switch (relationship.getRelationshipType()) {
        case CHILD:
            return getChildLink(type, id, relationship, request);
        case REFERENCE:
            return getReferenceLink(type, id, relationship, request);
        }

        return null;
    }

    protected Object getChildLink(String type, String id, Relationship relationship, ApiRequest request) {
        Schema otherSchema = request.getSchemaFactory().getSchema(relationship.getObjectType());
        if ( otherSchema == null ) {
            return Collections.EMPTY_LIST;
        }

        Object currentObject = getById(type, id, new ListOptions(request));
        if ( currentObject == null ) {
            return null;
        }

        String otherType = otherSchema.getId();
        Field field = otherSchema.getResourceFields().get(relationship.getPropertyName());
        if ( field == null ) {
            return Collections.EMPTY_LIST;
        }

        if ( otherSchema.getCollectionMethods().contains(Method.POST.toString()) ) {
            Map<String,Object> createDefaults = new HashMap<String, Object>();
            IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
            createDefaults.put(relationship.getPropertyName(), idFormatter.formatId(type, id));
            request.setCreateDefaults(createDefaults);
        }

        Map<Object,Object> criteria = getDefaultCriteria(false, otherType);
        criteria.put(relationship.getPropertyName(), id);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherType);
        return resourceManager.list(otherType, criteria, null);
    }

    protected Object getReferenceLink(String type, String id, Relationship relationship, ApiRequest request) {
        SchemaFactory schemaFactory = request.getSchemaFactory();
        Schema schema = schemaFactory.getSchema(type);
        Schema otherSchema = schemaFactory.getSchema(relationship.getObjectType());
        Field field = schema.getResourceFields().get(relationship.getPropertyName());
        if ( field == null || otherSchema == null ) {
            return null;
        }

        ListOptions options = new ListOptions(request);
        Object currentObject = getById(type, id, options);
        if ( currentObject == null ) {
            return null;
        }

        Object fieldValue = field.getValue(currentObject);

        if ( fieldValue == null ) {
            return null;
        }

        Map<Object,Object> criteria = getDefaultCriteria(true, otherSchema.getId());
        criteria.put(ObjectMetaDataManager.ID_FIELD, fieldValue);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherSchema.getId());
        return ApiUtils.getFirstFromList(resourceManager.list(otherSchema.getId(), criteria, options));
    }

    protected Map<String,Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type, Include include) {
        if ( include == null )
            return Collections.emptyMap();

        Map<String,Relationship> result = new HashMap<String, Relationship>();
        Map<String,Relationship> links = metaDataManager.getLinkRelationships(schemaFactory, type);
        for ( String link : include.getLinks() ) {
            link = link.toLowerCase();
            if ( links.containsKey(link) ) {
                result.put(link, links.get(link));
            }
        }

        return result;
    }

    @Override
    protected Map<Object,Object> getDefaultCriteria(boolean byId, String type) {
        Map<Object, Object> criteria = new HashMap<Object, Object>();
        Policy policy = ApiUtils.getPolicy();

        addAccountAuthorization(type, criteria, policy);

        if ( ! policy.isOption(Policy.REMOVED_VISIBLE) && ! byId ) {
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

    protected void addAccountAuthorization(String type, Map<Object, Object> criteria, Policy policy) {
        if ( ! policy.isOption(Policy.AUTHORIZED_FOR_ALL_ACCOUNTS) ) {
            List<Long> accounts = policy.getAuthorizedAccounts();
            if ( accounts.size() == 1 ) {
                criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, accounts.get(0));
            } else if ( accounts.size() == 0 ) {
                criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, policy.getAccountId());
            } else {
                criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, new Condition(ConditionType.IN, accounts));
            }
        }
    }

    @Override
    protected String getCollectionType(List<?> list, ApiRequest request) {
        String link = request.getLink();
        if ( link == null ) {
            return request.getType();
        } else {
            Relationship relationship = metaDataManager.getRelationship(request.getType(), link);
            return request.getSchemaFactory().getSchemaName(relationship.getObjectType());
        }
    }

    @Override
    protected Schema getSchemaForDisplay(SchemaFactory schemaFactory, Object obj) {
        return ApiUtils.getSchemaForDisplay(schemaFactory, obj);
    }

    @Override
    protected Resource constructResource(final IdFormatter idFormatter, SchemaFactory schemaFactory, final Schema schema, Object obj) {
        Map<String,Object> transitioningFields = metaDataManager.getTransitionFields(schema, obj);
        return ApiUtils.createResourceWithAttachments(schemaFactory, idFormatter, schema, obj, transitioningFields);
    }

    @Override
    protected Object resourceActionInternal(Object obj, ApiRequest request) {
        String processName = getProcessName(obj, request);
        ActionHandler handler = actionHandlersMap.get(processName);
        if ( handler != null ) {
            return handler.perform(processName, obj, request);
        }

        Map<String,Object> data = CollectionUtils.toMap(request.getRequestObject());

        try {
            scheduleProcess(getProcessName(obj, request), obj, data);
        } catch ( ProcessNotFoundException e ) {
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
        } catch ( ProcessInstanceException e ) {
            if ( e.getExitReason() == ExitReason.RESOURCE_BUSY || e.getExitReason() == ExitReason.CANCELED ) {
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void start() {
        actionHandlersMap = NamedUtils.createMapByName(actionHandlers);
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

}
