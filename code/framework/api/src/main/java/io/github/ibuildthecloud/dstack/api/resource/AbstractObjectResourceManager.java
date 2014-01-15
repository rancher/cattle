package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.engine.manager.ProcessNotFoundException;
import io.github.ibuildthecloud.dstack.engine.process.ExitReason;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstanceException;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.dstack.object.process.StandardProcess;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
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

public abstract class AbstractObjectResourceManager extends AbstractBaseResourceManager {

//    private static final Logger log = LoggerFactory.getLogger(AbstractObjectResourceManager.class);

    ObjectManager objectManager;
    ObjectProcessManager objectProcessManager;
    ObjectMetaDataManager metaDataManager;

    @Override
    protected Object authorize(Object object) {
        return ApiUtils.authorize(object);
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Class<?> clz = getClassForType(type);
        if ( clz == null ) {
            return null;
        }

        Map<String,Object> properties = new HashMap<String, Object>(CollectionUtils.<String, Object>toMap(request.getRequestObject()));
        if ( ! properties.containsKey(ObjectMetaDataManager.KIND_FIELD) ) {
            properties.put(ObjectMetaDataManager.KIND_FIELD, type);
        }

        Object result = objectManager.create(clz, properties);
        try {
            objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, result, properties);
            result = objectManager.reload(result);
        } catch ( ProcessInstanceException e ) {
            if ( e.getExitReason() == ExitReason.FAILED_TO_ACQUIRE_LOCK ) {
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            } else {
                throw e;
            }
        } catch ( ProcessNotFoundException e ) {
        }

        return result;
    }

    protected Class<?> getClassForType(String type) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        if ( clz == null ) {
            Schema schema = schemaFactory.getSchema(type);
            if ( schema.getParent() != null ) {
                return getClassForType(schema.getParent());
            }
        }

        return clz;
    }

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
        Schema otherSchema = schemaFactory.getSchema(relationship.getObjectType());
        if ( otherSchema == null ) {
            return Collections.EMPTY_LIST;
        }

        Object currentObject = getById(type, id, new ListOptions(request));
        if ( currentObject == null ) {
            return Collections.EMPTY_LIST;
        }

        String otherType = otherSchema.getId();
        Field field = otherSchema.getResourceFields().get(relationship.getPropertyName());
        if ( field == null ) {
            return Collections.EMPTY_LIST;
        }

        Map<Object,Object> criteria = getDefaultCriteria(false);
        criteria.put(relationship.getPropertyName(), id);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherType);
        return resourceManager.list(otherType, criteria, null);
    }

    protected Object getReferenceLink(String type, String id, Relationship relationship, ApiRequest request) {
        Schema schema = schemaFactory.getSchema(type);
        Schema otherSchema = schemaFactory.getSchema(relationship.getObjectType());
        Field field = schema.getResourceFields().get(relationship.getPropertyName());
        if ( field == null || otherSchema == null ) {
            return null;
        }

        ListOptions options = new ListOptions(request);
        Object currentObject = getById(type, id, options);
        Object fieldValue = field.getValue(currentObject);

        if ( fieldValue == null ) {
            return null;
        }

        Map<Object,Object> criteria = getDefaultCriteria(true);
        criteria.put(ObjectMetaDataManager.ID_FIELD, fieldValue);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherSchema.getId());
        return ApiUtils.getFirstFromList(resourceManager.list(otherSchema.getId(), criteria, options));
    }

    protected Map<String,Relationship> getLinkRelationships(String type, Include include) {
        if ( include == null )
            return Collections.emptyMap();

        Map<String,Relationship> result = new HashMap<String, Relationship>();
        Map<String,Relationship> links = metaDataManager.getLinkRelationships(schemaFactory, type);
        for ( String link : include.getLinks() ) {
            if ( links.containsKey(link) ) {
                result.put(link, links.get(link));
            }
        }

        return result;
    }

    @Override
    protected Map<Object,Object> getDefaultCriteria(boolean byId) {
        Map<Object, Object> criteria = new HashMap<Object, Object>();
        Policy policy = ApiUtils.getPolicy();

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

        if ( ! policy.isOption(Policy.REMOVED_VISIBLE) && ! byId ) {
            Condition or = new Condition(new Condition(ConditionType.NULL), new Condition(ConditionType.GTE, new Date()));
            criteria.put(ObjectMetaDataManager.REMOVE_TIME_FIELD, or);
        }

        return criteria;
    }

    @Override
    protected String getCollectionType(List<?> list, ApiRequest request) {
        String link = request.getLink();
        if ( link == null ) {
            return request.getType();
        } else {
            Relationship relationship = metaDataManager.getRelationship(request.getType(), link);
            return schemaFactory.getSchemaName(relationship.getObjectType());
        }
    }

    @Override
    protected Schema getSchemaForDisplay(Object obj) {
        return ApiUtils.getSchemaForDisplay(getSchemaFactory(), obj);
    }

    @Override
    protected Resource constructResource(final IdFormatter idFormatter, final Schema schema, Object obj) {
        Map<String,Object> transitioningFields = metaDataManager.getTransitionFields(schema, obj);
        return ApiUtils.createResourceWithAttachments(schemaFactory, idFormatter, schema, obj, transitioningFields);
    }

    @Override
    protected Object resourceActionInternal(Object obj, ApiRequest request) {
        Map<String,Object> data = CollectionUtils.toMap(request.getRequestObject());
        ProcessInstance pi = objectProcessManager.createProcessInstance(getProcessName(obj, request), obj, data);

        try {
            pi.schedule();
        } catch ( ProcessInstanceException e ) {
            if ( e.getExitReason() == ExitReason.FAILED_TO_ACQUIRE_LOCK || e.getExitReason() == ExitReason.CANCELED ) {
                throw new ClientVisibleException(ResponseCodes.CONFLICT);
            } else {
                throw e;
            }
        }

        request.setResponseCode(ResponseCodes.ACCEPTED);
        return objectManager.reload(obj);
    }

    protected String getProcessName(Object obj, ApiRequest request) {
        String baseType = schemaFactory.getBaseType(request.getType());
        return String.format("%s.%s", baseType == null ? request.getType() : baseType, request.getAction()).toLowerCase();
    }

    @Override
    protected Object collectionActionInternal(Object resources, ApiRequest request) {
        return null;
    }

    @Override
    protected Map<String, String> getLinks(Resource resource) {
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

}
