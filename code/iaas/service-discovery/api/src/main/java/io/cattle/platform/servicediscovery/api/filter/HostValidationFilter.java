package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.iaas.api.infrastructure.InfrastructureAccessManager;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class HostValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Inject
    ResourceManagerLocator locator;
    @Inject
    ObjectManager objMgr;
    @Inject
    InfrastructureAccessManager infraAccess;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Host.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        validateInfraAccess(request, "create");
        return super.create(type, request, next);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        validateInfraAccess(request, "update");
        return super.update(type, id, request, next);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        validateInfraAccess(request, "delete");
        return super.delete(type, id, request, next);
    }

    private void validateInfraAccess(ApiRequest request, String action) {
        if (!infraAccess.canModifyInfrastructure(ApiUtils.getPolicy())) {
            String message = String.format("Cannot %s host", action);
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "Forbidden", message, null);
        }
    }
}
