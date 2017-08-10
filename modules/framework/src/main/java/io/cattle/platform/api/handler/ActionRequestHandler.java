package io.cattle.platform.api.handler;

import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.ApiRequestHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import javax.servlet.ServletException;
import java.io.IOException;

public class ActionRequestHandler implements ApiRequestHandler {

    ResourceManagerLocator locator;
    ObjectManager objectManager;
    ObjectProcessManager processManager;

    public ActionRequestHandler(ResourceManagerLocator locator, ObjectManager objectManager, ObjectProcessManager processManager) {
        super();
        this.locator = locator;
        this.objectManager = objectManager;
        this.processManager = processManager;
    }

    @Override
    public void handle(ApiRequest request) throws IOException {
        Object response = null;
        String method = request.getMethod();

        if (Method.POST.isMethod(method) && request.getAction() != null) {
            if (request.getId() == null) {
                // Still don't support collection actions :(
            } else {
                response = resourceAction(request.getType(), request);
                request.setResponseObject(response);
            }
        }
    }

    private Object resourceAction(String type, ApiRequest request) {
        Object resource = getById(type, request.getId(), new ListOptions());
        if (resource == null) {
            return null;
        }

        return resourceActionInternal(resource, request);
    }

    private Object getById(String type, String id, ListOptions listOptions) {
        ResourceManager resourceManager = locator.getResourceManagerByType(type);
        return resourceManager.getById(type, id, listOptions);
    }

    private Object resourceActionInternal(Object obj, ApiRequest request) {
        ActionHandler handler = locator.getActionHandler(request.getAction(), request.getType());
        if (handler != null) {
            return handler.perform(obj, request);
        }
        return null;
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        return false;
    }

}
