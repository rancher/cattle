package io.cattle.platform.iaas.api.change.impl;

import io.cattle.platform.api.pubsub.subscribe.ApiPubSubEventPostProcessor;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.iaas.event.IaasEvents;
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
    public void processEvent(EventVO<Object> event) {
        if (event.getName() == null || !event.getName().startsWith(IaasEvents.RESOURCE_CHANGE)) {
            return;
        }

        String type = event.getResourceType();
        String id = event.getResourceId();

        if (type == null || id == null) {
            return;
        }

        ResourceManager rm = locator.getResourceManagerByType(type);

        if (rm == null) {
            return;
        }

        try {
            ApiRequest request = ApiContext.getContext().getApiRequest();

            Object obj = rm.getById(type, id, new ListOptions(request));

            if (obj == null) {
                return;
            }

            Resource resource = rm.convertResponse(obj, request);
            if (resource == null) {
                return;
            }

            Map<String, Object> data = new HashMap<String, Object>();
            data.put("resource", jsonMapper.convertValue(resource, Map.class));

            event.setData(data);
        } catch (ClientVisibleException e) {
        }
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
