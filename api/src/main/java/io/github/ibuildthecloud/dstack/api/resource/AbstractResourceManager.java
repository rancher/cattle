package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.api.auth.Policy;
import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.CollectionImpl;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public abstract class AbstractResourceManager implements ResourceManager {

    SchemaFactory schemaFactory;
    ObjectManager objectManager;
    ObjectMetaDataManager metaDataManager;
    ResourceManagerLocator locator;

    @Override
    public final Object getById(String type, String id) {
        return ApiUtils.authorize(getByIdInternal(type, id));
    }

    protected Object getByIdInternal(String type, String id) {
        Map<Object,Object> criteria = getDefaultCriteria(true);
        criteria.put(ObjectMetaDataManager.ID_FIELD, id);

        return ApiUtils.getFirstFromList(listInternal(type, criteria));
    }

    @Override
    public final Object list(String type, ApiRequest request) {
        return ApiUtils.authorize(listInternal(type, request));
    }

    protected Object listInternal(String type, ApiRequest request) {
        Map<Object,Object> criteria = getDefaultCriteria(false);
        criteria.putAll(request.getConditions());
        return listInternal(type, criteria);
    }

    @Override
    public final Object list(String type, Map<Object, Object> criteria) {
        return ApiUtils.authorize(listInternal(type, criteria));
    }

    protected abstract Object listInternal(String type, Map<Object, Object> criteria);

    @Override
    public final Object create(String type, ApiRequest request) {
        return ApiUtils.authorize(createInternal(type, request));
    }

    protected final Object createInternal(String type, ApiRequest request) {
        Class<?> clz = schemaFactory.getSchemaClass(request.getType());
        if ( clz == null ) {
            return null;
        }

        Object result = objectManager.create(clz, ApiUtils.getMap(request.getRequestObject()));
        return ApiUtils.authorize(result);
    }

    @Override
    public final Object getLink(String type, String id, String link, ApiRequest request) {
        return ApiUtils.authorize(getLinkInternal(type, id, link, request));
    }

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

        Object currentObject = getById(type, id);
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
        return resourceManager.list(otherType, criteria);
    }

    protected Object getReferenceLink(String type, String id, Relationship relationship, ApiRequest request) {
        Schema schema = schemaFactory.getSchema(type);
        Schema otherSchema = schemaFactory.getSchema(relationship.getObjectType());
        Field field = schema.getResourceFields().get(relationship.getPropertyName());
        if ( field == null || otherSchema == null ) {
            return null;
        }

        Object currentObject = getById(type, id);
        Object fieldValue = field.getValue(currentObject);

        if ( fieldValue == null ) {
            return null;
        }

        Map<Object,Object> criteria = getDefaultCriteria(true);
        criteria.put(ObjectMetaDataManager.ID_FIELD, fieldValue);

        ResourceManager resourceManager = locator.getResourceManagerByType(otherSchema.getId());
        return ApiUtils.getFirstFromList(resourceManager.list(otherSchema.getId(), criteria));
    }

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
    public Collection convertResponse(List<?> list, ApiRequest request) {
        return createCollection(list, request);
    }

    @Override
    public Resource convertResponse(Object obj, ApiRequest request) {
        return createResource(obj);
    }

    protected Collection createCollection(List<?> list, ApiRequest request) {
        CollectionImpl collection = new CollectionImpl();
        if ( request != null ) {
            String link = request.getLink();
            if ( link == null ) {
                collection.setResourceType(request.getType());
            } else {
                Relationship relationship = metaDataManager.getRelationship(request.getType(), link);
                collection.setResourceType(schemaFactory.getSchemaName(relationship.getObjectType()));
            }
        }

        for ( Object obj : list ) {
            Resource resource = createResource(obj);
            if ( resource != null ) {
                collection.getData().add(resource);
                if ( collection.getResourceType() == null ) {
                    collection.setResourceType(resource.getType());
                }
            }
        }

        return collection;
    }

    protected Resource createResource(Object obj) {
        if ( obj == null )
            return null;

        if ( obj instanceof Resource )
            return (Resource)obj;

        Schema schema = schemaFactory.getSchema(obj.getClass());
        if ( schema == null ) {
            return null;
        }

        WrappedResource resource = new WrappedResource(schema, obj);
        addLinks(resource);

        return resource;
    }

    protected void addLinks(WrappedResource resource) {
        Map<String,URL> links = resource.getLinks();
        for ( String linkName : metaDataManager.getLinks(schemaFactory, resource) ) {
            URL link = ApiContext.getUrlBuilder().resourceLink(resource, linkName);
            if ( link != null ) {
                links.put(linkName, link);
            }
        }
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
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

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

}
