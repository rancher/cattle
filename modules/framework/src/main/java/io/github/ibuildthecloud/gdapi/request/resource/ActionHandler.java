package io.github.ibuildthecloud.gdapi.request.resource;

import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ActionHandler {

    Object perform(Object obj, ApiRequest request);

}
