package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;

public class ResourceManagerRequestHandler extends AbstractResponseGenerator {

    ResourceManagerLocator resourceManagerLocator;

    @Override
    protected void generate(ApiRequest request) throws IOException {
        ResourceManager manager = resourceManagerLocator.getResourceManager(request);

        if (manager == null) {
            return;
        }

        /*
         * Note at this point we can assume type is not null because if type is null the manager will not be found
         */
        Object response = null;
        String method = request.getMethod();

        if (Method.POST.isMethod(method)) {
            if (request.getAction() == null) {
                /*
                 * Optimistically set response code to created. The ResourceManager impl should set the code to ACCEPTED if a background task was created. On
                 * error and exception should be thrown and then the response code will be changed to an error code
                 */
                request.setResponseCode(ResponseCodes.CREATED);
                response = manager.create(request.getType(), request);
            } else if (request.getId() == null) {
                response = manager.collectionAction(request.getType(), request);
            } else {
                response = manager.resourceAction(request.getType(), request);
            }
        } else if (Method.PUT.isMethod(method)) {
            response = manager.update(request.getType(), request.getId(), request);
        } else if (Method.GET.isMethod(method)) {
            if (request.getId() != null && request.getLink() == null) {
                response = manager.getById(request.getType(), request.getId(), new ListOptions(request));
            } else if (request.getType() != null && request.getLink() != null) {
                response = manager.getLink(request.getType(), request.getId(), request.getLink(), request);
            } else if (request.getType() != null) {
                response = manager.list(request.getType(), request);
            }
        } else if (Method.DELETE.isMethod(method)) {
            response = manager.delete(request.getType(), request.getId(), request);
        }
        request.setResponseObject(response);
    }

    @Override
    public boolean handleException(ApiRequest request, Throwable e) throws IOException, ServletException {
        ResourceManager manager = resourceManagerLocator.getResourceManager(request);

        if (manager == null) {
            return super.handleException(request, e);
        }

        return manager.handleException(e, request);
    }

    public ResourceManagerLocator getResourceManagerLocator() {
        return resourceManagerLocator;
    }

    @Inject
    public void setResourceManagerLocator(ResourceManagerLocator resourceManagerLocator) {
        this.resourceManagerLocator = resourceManagerLocator;
    }

}