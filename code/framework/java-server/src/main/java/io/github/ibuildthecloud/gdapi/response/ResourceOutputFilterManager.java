package io.github.ibuildthecloud.gdapi.response;

import io.github.ibuildthecloud.gdapi.model.Resource;

public interface ResourceOutputFilterManager {

    ResourceOutputFilter getOutputFilter(Resource resource);

}
