package io.cattle.platform.iaas.api.filter.containerevent;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

import javax.inject.Inject;

public class ContainerEventFilter extends AbstractDefaultResourceManagerFilter {

    public static final String HOST_PARAM = "hostId";
    public static final String VERIFY_AGENT = "CantVerifyAgent";

    @Inject
    AgentDao agentDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ContainerEvent.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        ContainerEvent event = request.proxyRequestObject(ContainerEvent.class);

        Policy policy = ApiUtils.getPolicy();
        /* Will never return null, MissingRequired will be thrown if missing */
        Agent agent = objectManager.loadResource(Agent.class, policy.getOption(Policy.AGENT_ID));

        Map<String, Host> hosts = agentDao.getHosts(agent.getId());
        Host host = hosts.get(event.getReportedHostUuid());
        if ( host == null ) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HOST_PARAM);
        }

        event.setHostId(host.getId());

        return super.create(type, request, next);
    }
}
