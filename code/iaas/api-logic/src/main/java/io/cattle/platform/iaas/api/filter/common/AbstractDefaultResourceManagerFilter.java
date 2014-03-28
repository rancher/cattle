package io.cattle.platform.iaas.api.filter.common;

import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractResourceManagerFilter;

public class AbstractDefaultResourceManagerFilter extends AbstractResourceManagerFilter implements Priority {

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

}
