package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.io.IOException;

public class ResourceManagerRequestHandler extends AbstractResponseGenerator {

    ResourceManagerLocator resourceManagerLocator;

    public ResourceManagerRequestHandler(ResourceManagerLocator resourceManagerLocator) {
        super();
        this.resourceManagerLocator = resourceManagerLocator;
    }

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
            } else {
                // Action, handled in ActionHandler
            }
        } else if (Method.PUT.isMethod(method)) {
            response = manager.update(request.getType(), request.getId(), request);
        } else if (Method.GET.isMethod(method)) {
            if (request.getId() != null && request.getLink() == null) {
                response = manager.getById(request.getType(), request.getId(), new ListOptions(request));
            } else if (request.getType() != null && request.getLink() != null) {
                // Link, handled in LinkHandler
            } else if (request.getType() != null) {
                response = manager.list(request.getType(), request);
            }
        } else if (Method.DELETE.isMethod(method)) {
            response = manager.delete(request.getType(), request.getId(), request);
        }
        request.setResponseObject(response);
    }

}