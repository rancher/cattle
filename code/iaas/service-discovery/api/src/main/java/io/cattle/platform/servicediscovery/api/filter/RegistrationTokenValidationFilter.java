package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Credential;
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
public class RegistrationTokenValidationFilter extends AbstractDefaultResourceManagerFilter {

    private static final String REG_TOKEN = "registrationToken";
    @Inject
    ResourceManagerLocator locator;
    @Inject
    ObjectManager objMgr;
    @Inject
    InfrastructureAccessManager infraAccess;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Credential.class };
    }

    @Override
    public String[] getTypes() {
        return new String[] { REG_TOKEN };
    }

    public Object create(String type, ApiRequest request, ResourceManager next) {
        validateInfraAccess(type);
        return next.create(type, request);
    }

    @Override
    public Object update(String type, String id, ApiRequest request, ResourceManager next) {
        validateInfraAccess(type);
        return next.update(type, id, request);
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        validateInfraAccess(type);
        return next.delete(type, id, request);
    }

    private void validateInfraAccess(String type) {
        if (REG_TOKEN.equalsIgnoreCase(type) && !infraAccess.canModifyInfrastructure(ApiUtils.getPolicy())) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, "Forbidden", "Cannot access registrationToken", null);
        }
    }
}
