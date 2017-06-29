package io.cattle.platform.iaas.api.filter.externalevent;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

public class ExternalEventFilter extends AbstractValidationFilter {

    public static final String VERIFY_AGENT = "CantVerifyAgent";

    ObjectManager objectManager;

    public ExternalEventFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Policy policy = ApiUtils.getPolicy();
        Agent agent = objectManager.loadResource(Agent.class, policy.getOption(Policy.AGENT_ID));
        if (agent == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        return super.create(type, request, next);
    }
}
