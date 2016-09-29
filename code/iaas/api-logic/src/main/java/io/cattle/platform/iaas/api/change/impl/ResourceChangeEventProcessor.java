package io.cattle.platform.iaas.api.change.impl;

import io.cattle.platform.api.pubsub.subscribe.ApiPubSubEventPostProcessor;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ResourceChangeEventProcessor implements ApiPubSubEventPostProcessor {

    ResourceManagerLocator locator;
    JsonMapper jsonMapper;

    @Override
    public boolean processEvent(EventVO<Object> event) {
        if (event.getName() == null || !event.getName().startsWith(IaasEvents.RESOURCE_CHANGE)) {
            return true;
        }

        String type = event.getResourceType();
        String id = event.getResourceId();

        if (type == null || id == null) {
            return false;
        }

        ResourceManager rm = locator.getResourceManagerByType(type);

        if (rm == null) {
            return false;
        }

        try {
            ApiRequest request = ApiContext.getContext().getApiRequest();

            Object obj = rm.getById(type, id, new ListOptions(request));

            if (obj == null) {
                return false;
            }

            request.setResponseObject(obj);
            Resource resource = rm.convertResponse(obj, request);
            if (resource == null) {
                return false;
            }

            Object removed = resource.getFields().get(ObjectMetaDataManager.REMOVED_FIELD);
            String state = DataAccessor.fromMap(resource.getFields()).withKey(ObjectMetaDataManager.STATE_FIELD).as(String.class);
            if (removed != null && !CommonStatesConstants.REMOVED.equals(state)) {
                return false;
            }

            Map<String, Object> data = new HashMap<String, Object>();
            @SuppressWarnings("unchecked")
            Map<String, Object> resourceData = jsonMapper.convertValue(resource, Map.class);;
            if (request.getOptions().get("_actionLinks") != null) {
                data.put("actionLinks", resourceData.remove("actions"));
            }
            data.put("resource", resourceData);

            event.setResourceType(resource.getType());
            event.setData(data);
        } catch (ClientVisibleException e) {
        }

        return true;
    }

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

}
