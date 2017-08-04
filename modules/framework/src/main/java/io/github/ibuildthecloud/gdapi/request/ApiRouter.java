package io.github.ibuildthecloud.gdapi.request;

import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ValidationFilter;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.List;

public interface ApiRouter {

    ApiRouter handler(ApiRequestHandler handler);

    List<ApiRequestHandler> getHandlers();

    ApiRouter handlers(ApiRequestHandler... handlers);

    ApiRouter resourceManager(Object type, ResourceManager rm);

    ApiRouter validationFilter(Object type, ValidationFilter filter);

    ApiRouter outputFilter(Object type, ResourceOutputFilter filter);

    ApiRouter link(Object type, LinkHandler link);

    ApiRouter action(Object type, String name, ActionHandler action);

    ApiRouter defaultResourceManager(ResourceManager rm);

    ApiRouter defaultActionHandler(ActionHandler action);

}
