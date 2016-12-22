package io.cattle.platform.iaas.api.filter.containerevent;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.ContainerEventDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContainerEventFilter extends AbstractDefaultResourceManagerFilter {

    public static final String HOST_PARAM = "hostId";
    public static final String VERIFY_AGENT = "CantVerifyAgent";
    public static final String TOO_MANY = "TooManyContainerEvents";
    private static final Logger log = LoggerFactory.getLogger(ContainerEventFilter.class);

    @Inject
    AgentDao agentDao;

    @Inject
    ContainerEventDao containerEventDao;

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
        Agent agent = objectManager.loadResource(Agent.class, policy.getOption(Policy.AGENT_ID));
        if (agent == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        Map<String, Host> hosts = agentDao.getHosts(agent.getId());
        Host host = hosts.get(event.getReportedHostUuid());
        if ( host == null ) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HOST_PARAM);
        }

        if (!containerEventDao.canCreate(host.getId(), event.getExternalStatus())) {
            log.info("Dropping container event from agent for host [{}]", host.getId());
            throw new ClientVisibleException(ResponseCodes.CONFLICT, TOO_MANY);
        }

        event.setHostId(host.getId());

        return super.create(type, request, next);
    }
}
