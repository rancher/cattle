package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;
import java.util.Map;

public class RequestReRouterHandler extends AbstractApiRequestHandler {

    @Override
    public void handle(ApiRequest request) throws IOException {
        String version = request.getVersion();
        if (version == null) {
            return;
        }

        switch(version) {
        case "v1":
            routeV1(request);
        }
    }

    protected void routeV1(ApiRequest request) throws IOException {
        if (request.getType() == null) {
            return;
        }

        switch (request.getType()) {
        case "environment":
        case "kubernetesStack":
            routeV1Environment(request);
            break;
        case ServiceConstants.KIND_SERVICE:
        case ServiceConstants.KIND_KUBERNETES_SERVICE:
        case ServiceConstants.KIND_LOAD_BALANCER_SERVICE:
        case ServiceConstants.KIND_DNS_SERVICE:
        case ServiceConstants.KIND_EXTERNAL_SERVICE:
        case ServiceConstants.KIND_SELECTOR_SERVICE:
        case ServiceConstants.KIND_SCALING_GROUP_SERVICE:
            routeV1Service(request);
            break;
        }
    }

    protected void routeV1Environment(ApiRequest request) throws IOException {
        request.setSchemaVersion("v2-beta");
        if ("environment".equals(request.getType())) {
            request.setType("stack");
        }
    }

    protected void routeV1Service(ApiRequest request) throws IOException {
        Map<String, Object> body = CollectionUtils.toMap(request.getRequestObject());
        Object value = body.get("environmentId");
        if (value != null){
            body.put("stackId", value);
        }
        request.setSchemaVersion("v2-beta");
    }

}