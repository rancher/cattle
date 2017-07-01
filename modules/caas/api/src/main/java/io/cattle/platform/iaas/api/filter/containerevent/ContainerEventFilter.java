package io.cattle.platform.iaas.api.filter.containerevent;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.containersync.model.ContainerEventEvent;
import io.cattle.platform.core.addon.ContainerEvent;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

public class ContainerEventFilter extends AbstractValidationFilter {

    public static final String HOST_PARAM = "hostId";
    public static final String VERIFY_AGENT = "CantVerifyAgent";
    public static final String TOO_MANY = "TooManyContainerEvents";

    AgentDao agentDao;
    ObjectManager objectManager;
    JsonMapper jsonMapper;
    EventService eventService;

    public ContainerEventFilter(AgentDao agentDao, ObjectManager objectManager) {
        super();
        this.agentDao = agentDao;
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        ContainerEvent event = jsonMapper.convertValue(request.getRequestObject(), ContainerEvent.class);

        Policy policy = ApiUtils.getPolicy();
        Agent agent = objectManager.loadResource(Agent.class, policy.getOption(Policy.AGENT_ID));
        if (agent == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        Host host = agentDao.getHost(agent.getId(), event.getReportedHostUuid());
        if (host == null) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HOST_PARAM);
        }

        event.setHostId(host.getId());
        event.setAccountId(host.getAccountId());

        eventService.publish(new ContainerEventEvent(event));
        return null;
    }
}
