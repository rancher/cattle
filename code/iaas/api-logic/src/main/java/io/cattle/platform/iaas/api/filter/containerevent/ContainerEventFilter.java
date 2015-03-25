package io.cattle.platform.iaas.api.filter.containerevent;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.dao.AgentDao;
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

import org.jooq.exception.InvalidResultException;

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
        Agent agent = null;
        try {
            agent = objectManager.findOne(Agent.class, AGENT.ACCOUNT_ID, policy.getAccountId());
        } catch (InvalidResultException e) {
            // Found more than one agent, reject request just as if we didn't
            // find any.
        }

        if ( agent == null ) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        Map<String, Host> hosts = agentDao.getHosts(agent.getId());
        Host host = hosts.get(event.getReportedHostUuid());
        if ( host == null ) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HOST_PARAM);
        }

        event.setHostId(host.getId());

        return super.create(type, request, next);
    }
}
