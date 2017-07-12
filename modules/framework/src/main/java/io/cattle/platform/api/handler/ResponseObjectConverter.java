package io.cattle.platform.api.handler;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ActionDefinition;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.model.impl.CollectionImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.response.ResponseConverter;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

public class ResponseObjectConverter implements ApiRequestHandler, ResponseConverter {

    Map<String, Map<String, String>> linksCache = Collections.synchronizedMap(new WeakHashMap<String, Map<String, String>>());
    ObjectMetaDataManager metaDataManager;
    ObjectManager objectManager;
    ResourceManagerLocator locator;

    public ResponseObjectConverter(ObjectMetaDataManager metaDataManager, ObjectManager objectManager, ResourceManagerLocator locator) {
        super();
        this.metaDataManager = metaDataManager;
        this.objectManager = objectManager;
        this.locator = locator;
    }

    @Override
    public void handle(ApiRequest request) throws IOException {
        Object response = request.getResponseObject();
        if (response == null)
            return;

        if (response instanceof Resource || response instanceof Collection || response instanceof InputStream) {
            return;
        }

        if (response instanceof List) {
            response = createCollection((List<?>) response, request);
        } else {
            response = convertResponse(response, request);
        }

        request.setResponseObject(response);
    }

    @Override
    public Resource convertResponse(Object obj, ApiRequest request) {
        Resource resource = createResource(obj, ApiContext.getContext().getIdFormatter(), request);
        ResourceOutputFilter filter = locator.getOutputFilter(resource);

        if (filter != null) {
            resource = filter.filter(request, obj, resource);
        }

        return resource;
    }

    protected Collection createCollection(List<?> list, ApiRequest request) {
        CollectionImpl collection = new CollectionImpl();
        if (request != null) {
            String collectionType = getCollectionType(list, request);
            collection.setResourceType(collectionType);

            Schema schema = request.getSchemaFactory().getSchema(collectionType);
            if (schema != null && schema.getChildren().size() > 0) {
                UrlBuilder urlBuilder = ApiContext.getUrlBuilder();
                Map<String, URL> createTypes = new TreeMap<>();

                if (schema.getCollectionMethods().contains(Method.POST.toString())) {
                    createTypes.put(schema.getId(), urlBuilder.resourceCollection(collectionType));
                }

                for (String childType : schema.getChildren()) {
                    schema = request.getSchemaFactory().getSchema(childType);

                    if (schema != null && schema.getCollectionMethods().contains(Method.POST.toString())) {
                        createTypes.put(schema.getId(), urlBuilder.resourceCollection(schema.getId()));
                    }
                }

                if (createTypes.size() > 0) {
                    collection.setCreateTypes(createTypes);
                }
            }
        }

        collection.setCreateDefaults(request.getCreateDefaults());

        addSort(collection, request);
        addPagination(list, collection, request);
        addFilters(collection, request);

        for (Object obj : list) {
            Resource resource = convertResponse(obj, request);
            if (resource != null) {
                collection.getData().add(resource);
                if (collection.getResourceType() == null) {
                    collection.setResourceType(resource.getType());
                }
            }
        }

        return collection;
    }

    protected String getCollectionType(List<?> list, ApiRequest request) {
        String link = request.getLink();
        if (link == null) {
            return request.getType();
        } else {
            Relationship relationship = metaDataManager.getRelationship(request.getSchemaFactory()
                    .getSchemaClass(request.getType(), true), link);
            if (relationship != null) {
                return request.getSchemaFactory().getSchemaName(relationship.getObjectType());
            }
        }

        return null;
    }

    protected void addSort(CollectionImpl collection, ApiRequest request) {
        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();
        Set<String> sortLinks = getSortLinks(request.getSchemaFactory(), collection.getResourceType());
        Map<String, URL> sortLinkMap = new TreeMap<>();
        for (String sortLink : sortLinks) {
            URL sortUrl = urlBuilder.sort(sortLink);
            if (sortUrl != null) {
                sortLinkMap.put(sortLink, sortUrl);
            }
        }

        collection.setSortLinks(sortLinkMap);
        collection.setSort(request.getSort());
    }

    protected Set<String> getSortLinks(SchemaFactory schemaFactory, String type) {
        String key = schemaFactory.getId() + ":sortlinks:" + type;
        Map<String, String> links = linksCache.get(key);
        if (links != null)
            return links.keySet();

        links = new HashMap<>();
        Schema schema = schemaFactory.getSchema(type);
        if (schema == null) {
            return Collections.emptySet();
        }

        for (String name : schema.getCollectionFilters().keySet()) {
            links.put(name, name);
        }

        linksCache.put(key, links);
        return links.keySet();
    }

    protected void addPagination(List<?> list, CollectionImpl collection, ApiRequest request) {
        Pagination pagination = request.getPagination();
        if (pagination == null) {
            return;
        }

        collection.setPagination(pagination.getResponse());
    }

    public Resource createResource(Object obj, IdFormatter idFormatter, ApiRequest apiRequest) {
        if (obj == null)
            return null;

        if (obj instanceof Resource)
            return (Resource)obj;

        SchemaFactory schemaFactory = apiRequest == null ? ApiContext.getSchemaFactory() : apiRequest.getSchemaFactory();

        Schema schema = getSchemaForDisplay(schemaFactory, obj);
        if (schema == null) {
            return null;
        }

        Resource resource = constructResource(idFormatter, schemaFactory, schema, obj, apiRequest);
        addLinks(obj, schemaFactory, schema, resource);
        addActions(obj, schemaFactory, schema, resource);

        return resource;
    }

    protected Schema getSchemaForDisplay(SchemaFactory schemaFactory, Object obj) {
        String schemaId = ApiUtils.getSchemaIdForDisplay(schemaFactory, obj);
        Schema schema = schemaFactory.getSchema(schemaId);

        if (schema == null) {
            /* Check core schema because the parent might not be authorized */
            schemaId = ApiUtils.getSchemaIdForDisplay(objectManager.getSchemaFactory(), obj);

            /* Still get schema from request's schemaFactory */
            schema = schemaFactory.getSchema(schemaId);
        }

        return schema;
    }

    protected Resource constructResource(IdFormatter idFormatter, SchemaFactory schemaFactory, Schema schema, Object obj, ApiRequest apiRequest) {
        Map<String, Object> transitioningFields = metaDataManager.getTransitionFields(schema, obj);
        return ApiUtils.createResource(apiRequest, idFormatter, schemaFactory, schema, obj, transitioningFields);
    }

    protected void addLinks(Object obj, SchemaFactory schemaFactory, Schema schema, Resource resource) {
        Map<String, URL> links = resource.getLinks();

        for (Map.Entry<String, String> entry : getLinks(schemaFactory, resource).entrySet()) {
            String linkName = entry.getKey();
            String propName = entry.getValue();

            URL link = ApiContext.getUrlBuilder().resourceLink(resource, linkName);
            if (link == null) {
                continue;
            }

            if (propName != null && resource.getFields().get(propName) == null) {
                continue;
            }

            links.put(linkName, link);
        }
    }

    protected void addActions(Object obj, SchemaFactory schemaFactory, Schema schema, Resource resource) {
        Object state = resource.getFields().get(ObjectMetaDataManager.STATE_FIELD);
        Map<String, ActionDefinition> defs = metaDataManager.getActionDefinitions(obj);

        if (state == null || defs == null) {
            Map<String, Action> actions = schema.getResourceActions();

            if (actions == null || actions.size() == 0) {
                return;
            }

            UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

            for (String name : actions.keySet()) {
                resource.getActions().put(name, urlBuilder.actionLink(resource, name));
            }
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

            if ("restore".equals(name) || !isValidAction(obj, action)) {
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

        String capability = Objects.toString(attributes.get("capability"), null);
        String state = Objects.toString(attributes.get(ObjectMetaDataManager.STATE_FIELD), null);
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
        return !(states != null && !states.contains(currentState));
    }

    protected void addFilters(CollectionImpl collection, ApiRequest request) {
        Schema schema = request.getSchemaFactory().getSchema(collection.getResourceType());
        if (schema == null) {
            return;
        }

        Map<String, List<Condition>> conditions = new TreeMap<>(request.getConditions());
        for (String key : schema.getCollectionFilters().keySet()) {
            if (!conditions.containsKey(key)) {
                conditions.put(key, null);
            }
        }
        collection.setFilters(conditions);
    }

    protected Map<String, String> getLinks(SchemaFactory schemaFactory, Resource resource) {
        return metaDataManager.getLinks(schemaFactory, resource.getType());
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        return false;
    }

}
