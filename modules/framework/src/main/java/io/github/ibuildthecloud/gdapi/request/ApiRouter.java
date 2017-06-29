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

    ApiRouter resourceManager(Class<?> type, ResourceManager rm);

    ApiRouter resourceManager(String type, ResourceManager rm);

    ApiRouter filter(Class<?> type, ValidationFilter filter);

    ApiRouter filter(String type, ValidationFilter filter);

    ApiRouter outputFilter(Class<?> type, ResourceOutputFilter filter);

    ApiRouter outputFilter(String type, ResourceOutputFilter filter);

    ApiRouter link(Class<?> type, LinkHandler link);

    ApiRouter link(String type, LinkHandler link);

    ApiRouter action(String name, ActionHandler action);

    ApiRouter defaultResourceManager(ResourceManager rm);

    ApiRouter defaultActionHandler(ActionHandler action);

}
