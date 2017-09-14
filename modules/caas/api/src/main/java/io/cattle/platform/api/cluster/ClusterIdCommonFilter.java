package io.cattle.platform.api.cluster;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

public class ClusterIdCommonFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public ClusterIdCommonFilter(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        ClusterAccessor accessor = request.proxyRequestObject(ClusterAccessor.class);
        Long clusterId = accessor.getClusterId();
        if (clusterId != null) {
            return next.create(type, request);
        }

        Account account = objectManager.loadResource(Account.class, ApiUtils.getPolicy().getAccountId());
        if (account == null) {
            return next.create(type, request);
        }

        if (account.getClusterId() == null) {
            throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_REFERENCE, "Create cluster first", null);
        }

        accessor.setClusterId(account.getClusterId());
        return next.create(type, request);
    }

    public interface ClusterAccessor {
        Long getClusterId();
        void setClusterId(Long clusterId);
    }
}
