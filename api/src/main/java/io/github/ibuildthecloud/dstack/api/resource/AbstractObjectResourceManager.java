package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractBaseResourceManager;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.collections.Transformer;

public abstract class AbstractObjectResourceManager extends AbstractBaseResourceManager {

    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;

    @Override
    protected Object authorize(Object object) {
        return ApiUtils.authorize(object);
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        if ( clz == null ) {
            return null;
        }

        Object result = objectManager.create(clz, ApiUtils.getMap(request.getRequestObject()));
        return result;
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        Map<String,Object> updates = ApiUtils.getMap(request.getRequestObject());
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

        if ( ! policy.isAuthorizedForAllAccounts() ) {
            List<Long> accounts = policy.getAuthorizedAccounts();
            if ( accounts.size() == 1 ) {
                criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, accounts.get(0));
            } else {
                criteria.put(ObjectMetaDataManager.ACCOUNT_FIELD, new Condition(ConditionType.IN, accounts));
            }
        }

        if ( ! policy.isRemovedVisible() && ! byId ) {
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
    protected Resource constructResource(final IdFormatter idFormatter, final Schema schema, Object obj) {
        Map<String,Object> additionalFields = new LinkedHashMap<String, Object>();
        additionalFields.putAll(DataUtils.getFields(obj));

        Map<String,Object> attachments = ApiUtils.getAttachements(obj, new Transformer() {
            @Override
            public Object transform(Object input) {
                input = ApiUtils.authorize(input);
                if ( input == null )
                    return null;

                Schema schema = schemaFactory.getSchema(input.getClass());
                if ( schema == null ) {
                    return null;
                }

                return new WrappedResource(idFormatter, schema, input, DataUtils.getFields(input));
            }
        });

        additionalFields.putAll(attachments);

        return new WrappedResource(idFormatter, schema, obj, additionalFields);
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

}
