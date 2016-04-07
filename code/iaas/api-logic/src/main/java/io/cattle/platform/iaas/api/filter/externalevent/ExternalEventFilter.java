package io.cattle.platform.iaas.api.filter.externalevent;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

public class ExternalEventFilter extends AbstractDefaultResourceManagerFilter {

    public static final String VERIFY_AGENT = "CantVerifyAgent";

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { "externalEvent", "externalStoragePoolEvent", "externalVolumeEvent", "externalServiceEvent" };
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
