package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

public interface ResourceOutputFilter {

    Resource filter(ApiRequest request, Object original, Resource converted);

    String[] getTypes();

    Class<?>[] getTypeClasses();

}
