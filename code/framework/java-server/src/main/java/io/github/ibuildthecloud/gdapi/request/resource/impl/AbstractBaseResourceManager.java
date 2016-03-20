package io.github.ibuildthecloud.gdapi.request.resource.impl;

import io.github.ibuildthecloud.gdapi.condition.Condition;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Pagination;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.model.impl.CollectionImpl;
import io.github.ibuildthecloud.gdapi.model.impl.WrappedResource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilterManager;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;
import io.github.ibuildthecloud.gdapi.util.TypeUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public abstract class AbstractBaseResourceManager implements ResourceManager {

    private static final String DEFAULT_CRITERIA = " _defaultCriteria";

    Map<String, Map<String, String>> linksCache = Collections.synchronizedMap(new WeakHashMap<String, Map<String, String>>());
    Set<Class<?>> resourcesToCreate = new HashSet<Class<?>>();
    protected ResourceManagerLocator locator;
    protected ResourceOutputFilterManager outputFilterManager;

    protected Object authorize(Object object) {
        return object;
    }

    protected void addResourceToCreateResponse(Class<?> clz) {
        this.resourcesToCreate.add(clz);
    }

    @Override
    public final Object getById(String type, String id, ListOptions options) {
        return authorize(getByIdInternal(type, id, options));
    }

    protected Object getByIdInternal(String type, String id, ListOptions options) {
        Map<Object, Object> criteria = getDefaultCriteria(true, false, type);
        criteria.put(TypeUtils.ID_FIELD, id);

        return getFirstFromList(listInternal(ApiContext.getSchemaFactory(), type, criteria, options));
    }

    @Override
    public final Object list(String type, ApiRequest request) {
        return list(type, new LinkedHashMap<Object, Object>(request.getConditions()), new ListOptions(request));
    }

    @Override
    public final List<?> list(String type, Map<Object, Object> criteria, ListOptions options) {
        if (!isDefaultCriteria(criteria)) {
            criteria = mergeCriteria(criteria, getDefaultCriteria(false, false, type));
        }

        Object result = authorize(listInternal(ApiContext.getSchemaFactory(), type, criteria, options));
        return RequestUtils.toList(result);
    }

    protected Map<Object, Object> mergeCriteria(Map<Object, Object> criteria, Map<Object, Object> other) {
        if (criteria == null) {
            criteria = new LinkedHashMap<Object, Object>();
        }

        for (Map.Entry<Object, Object> entry : other.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            Object existing = criteria.get(key);

            if (existing instanceof List) {
                List<Object> newCondition = new ArrayList<Object>();
                newCondition.add(value);
                newCondition.addAll((List<?>)existing);
                criteria.put(key, newCondition);
            } else if (existing == null) {
                criteria.put(key, value);
            } else {
                criteria.put(key, Arrays.asList(value, existing));
            }
        }

        return criteria;
    }

    protected abstract Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options);

    @Override
    public final Object create(String type, ApiRequest request) {
        return authorize(createInternal(type, request));
    }

    protected abstract Object createInternal(String type, ApiRequest request);

    @Override
    public final Object update(String type, String id, ApiRequest request) {
        Object object = getById(type, id, new ListOptions(request));
        if (object == null) {
            return null;
        }

        return updateInternal(type, id, object, request);
    }

    protected abstract Object updateInternal(String type, String id, Object obj, ApiRequest request);

    @Override
    public final Object delete(String type, String id, ApiRequest request) {
        Object object = getById(type, id, new ListOptions(request));
        if (object == null) {
            return null;
        }

        return deleteInternal(type, id, object, request);
    }

    protected abstract Object deleteInternal(String type, String id, Object obj, ApiRequest request);

    @Override
    public final Object getLink(String type, String id, String link, ApiRequest request) {
        return authorize(getLinkInternal(type, id, link, request));
    }

    protected abstract Object getLinkInternal(String type, String id, String link, ApiRequest request);

    protected Map<Object, Object> getDefaultCriteria(boolean byId, boolean byLink, String type) {
        Map<Object, Object> result = new HashMap<Object, Object>();
        result.put(DEFAULT_CRITERIA, true);
        return result;
    }

    protected boolean isDefaultCriteria(Map<Object, Object> criteria) {
        return criteria != null && criteria.containsKey(DEFAULT_CRITERIA);
    }

    protected Object getMarker(Pagination pagination) {
        if (pagination == null) {
            return null;
        }

        String marker = pagination.getMarker();
        if (StringUtils.isBlank(marker)) {
            return null;
        }

        if (marker.charAt(0) == 'm') {
            return marker.substring(1);
        }

        Object obj = ApiContext.getContext().getIdFormatter().parseId(marker);
        if (obj instanceof Long) {
            return obj;
        } else if (obj != null) {
            try {
                return new Long(obj.toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        return null;
    }

    @Override
    public Collection convertResponse(List<?> list, ApiRequest request) {
        return createCollection(list, request);
    }

    @Override
    public Resource convertResponse(Object obj, ApiRequest request) {
        Resource resource = createResource(obj, ApiContext.getContext().getIdFormatter(), request);
        ResourceOutputFilter filter = outputFilterManager.getOutputFilter(resource);

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
                Map<String, URL> createTypes = new TreeMap<String, URL>();

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
        return request.getType();
    }

    protected void addFilters(CollectionImpl collection, ApiRequest request) {
        Schema schema = request.getSchemaFactory().getSchema(collection.getResourceType());
        Map<String, List<Condition>> conditions = new TreeMap<String, List<Condition>>(request.getConditions());
        for (String key : schema.getCollectionFilters().keySet()) {
            if (!conditions.containsKey(key)) {
                conditions.put(key, null);
            }
        }
        collection.setFilters(conditions);
    }

    protected void addPagination(List<?> list, CollectionImpl collection, ApiRequest request) {
        Pagination pagination = request.getPagination();
        if (pagination == null) {
            return;
        }

        collection.setPagination(pagination.getResponse());
    }

    protected void addSort(CollectionImpl collection, ApiRequest request) {
        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();
        Set<String> sortLinks = getSortLinks(request.getSchemaFactory(), collection.getResourceType());
        Map<String, URL> sortLinkMap = new TreeMap<String, URL>();
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

        links = new HashMap<String, String>();
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

    protected Resource createResource(Object obj, IdFormatter idFormatter, ApiRequest apiRequest) {
        if (obj == null)
            return null;

        if (obj instanceof Resource)
            return (Resource)obj;

        SchemaFactory schemaFactory = apiRequest == null ? ApiContext.getSchemaFactory() : apiRequest.getSchemaFactory();

        if (resourcesToCreate.size() > 0 && !resourcesToCreate.contains(obj.getClass())) {
            String type = schemaFactory.getSchemaName(obj.getClass());
            ResourceManager rm = locator.getResourceManagerByType(type);
            if (rm != null) {
                return rm.convertResponse(obj, apiRequest);
            }
        }

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
        return schemaFactory.getSchema(obj.getClass());
    }

    protected void addActions(Object obj, SchemaFactory schemaFactory, Schema schema, Resource resource) {
        Map<String, Action> actions = schema.getResourceActions();

        if (actions == null || actions.size() == 0) {
            return;
        }

        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

        for (String name : actions.keySet()) {
            resource.getActions().put(name, urlBuilder.actionLink(resource, name));
        }
    }

    protected Resource constructResource(IdFormatter idFormatter, SchemaFactory schemaFactory, Schema schema, Object obj, ApiRequest apiRequest) {
        return new WrappedResource(idFormatter, schemaFactory, schema, obj, apiRequest.getMethod());
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

    protected Map<String, String> getLinks(SchemaFactory schemaFactory, Resource resource) {
        return new HashMap<String, String>();
    }

    public static Object getFirstFromList(Object obj) {
        if (obj instanceof Collection) {
            return getFirstFromList(((Collection)obj).getData());
        }

        if (obj instanceof List) {
            List<?> list = (List<?>)obj;
            return list.size() > 0 ? list.get(0) : null;
        }

        return null;
    }

    @Override
    public boolean handleException(Throwable t, ApiRequest request) {
        return false;
    }

    @Override
    public final Object resourceAction(String type, ApiRequest request) {
        Object resource = getById(type, request.getId(), new ListOptions());

        if (resource == null) {
            return null;
        }

        return resourceActionInternal(resource, request);
    }

    protected abstract Object resourceActionInternal(Object obj, ApiRequest request);

    @Override
    public final Object collectionAction(String type, ApiRequest request) {
        Object resources = list(type, request);
        if (resources == null) {
            return null;
        }

        return collectionActionInternal(resources, request);
    }

    protected abstract Object collectionActionInternal(Object resources, ApiRequest request);

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

    public ResourceOutputFilterManager getOutputFilterManager() {
        return outputFilterManager;
    }

    @Inject
    public void setOutputFilterManager(ResourceOutputFilterManager outputFilterManager) {
        this.outputFilterManager = outputFilterManager;
    }

}
