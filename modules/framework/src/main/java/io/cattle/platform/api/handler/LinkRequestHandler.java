package io.cattle.platform.api.handler;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkRequestHandler implements ApiRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(LinkRequestHandler.class);

    ResourceManagerLocator locator;
    ObjectMetaDataManager metadataManager;

    public LinkRequestHandler(ResourceManagerLocator locator, ObjectMetaDataManager metadataManager) {
        super();
        this.locator = locator;
        this.metadataManager = metadataManager;
    }

    @Override
    public void handle(ApiRequest request) throws IOException {
        /*
         * Note at this point we can assume type is not null because if type is null the manager will not be found
         */
        Object response = null;
        String method = request.getMethod();

        if (Method.GET.isMethod(method) && request.getId() != null && request.getType() != null && request.getLink() != null) {
            response = getLink(request.getType(), request.getId(), request.getLink(), request);
        }

        if (response != null) {
            request.setResponseObject(response);
        }
    }

    private Object getLink(String type, String id, String link, ApiRequest request) {
        List<LinkHandler> linkHandlers = locator.getLinkHandlersByType(type);
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
            relationship = metadataManager.getRelationship(clz, link);
        } else {
            relationship = metadataManager.getRelationship(type, link);
        }

        if (relationship == null) {
            return null;
        }

        switch (relationship.getRelationshipType()) {
        case CHILD:
            return getChildLink(type, id, relationship, request);
        case REFERENCE:
            return getReferenceLink(type, id, relationship, request);
        default:
            return null;
        }
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
            Map<String, Object> createDefaults = new HashMap<>();
            IdFormatter idFormatter = ApiContext.getContext().getIdFormatter();
            createDefaults.put(relationship.getPropertyName(), idFormatter.formatId(type, id));
            request.setCreateDefaults(createDefaults);
        }

        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(relationship.getPropertyName(), id);

        return list(otherType, criteria, null);
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

        Map<Object, Object> criteria = new HashMap<>();
        criteria.put(ObjectMetaDataManager.ID_FIELD, fieldValue);

        return ApiUtils.getFirstFromList(list(otherSchema.getId(), criteria, options));
    }

    protected Object list(String type, Map<Object, Object> criteria, ListOptions options) {
        ResourceManager resourceManager = locator.getResourceManagerByType(type);
        return resourceManager.list(type, criteria, options);
    }

    private Object getById(String type, String id, ListOptions listOptions) {
        ResourceManager resourceManager = locator.getResourceManagerByType(type);
        return resourceManager.getById(type, id, listOptions);
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        return false;
    }

}
