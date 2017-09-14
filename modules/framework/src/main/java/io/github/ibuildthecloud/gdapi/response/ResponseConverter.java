package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ResponseConverter {

    Resource convertResponse(Object obj, ApiRequest request);

}
