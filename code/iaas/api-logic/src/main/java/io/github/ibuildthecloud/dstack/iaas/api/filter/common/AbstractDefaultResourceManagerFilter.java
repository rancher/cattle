package io.github.ibuildthecloud.dstack.iaas.api.filter.common;

import io.github.ibuildthecloud.dstack.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;

public class AbstractDefaultResourceManagerFilter extends AbstractResourceManagerFilter implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
