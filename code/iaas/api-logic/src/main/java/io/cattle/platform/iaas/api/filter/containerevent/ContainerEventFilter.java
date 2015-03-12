package io.cattle.platform.iaas.api.filter.containerevent;

import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.ContainerEvent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import javax.inject.Inject;

import org.jooq.exception.InvalidResultException;

public class ContainerEventFilter extends AbstractDefaultResourceManagerFilter {
    
    public static final String HOST_PARAM = "hostId";
    
    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ContainerEvent.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        ContainerEvent event = request.proxyRequestObject(ContainerEvent.class);
        Host host = null;
        try {
            host = objectManager.findOne(Host.class, HOST.UUID, event.getReportedHostUuid());
        } catch(InvalidResultException e) {
            // Found more than one host, reject request just as if we didn't find any.
        }
        
        if (host == null) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HOST_PARAM);
        }
        
        Policy policy = ApiUtils.getPolicy();
        Agent agent = objectManager.loadResource(Agent.class, host.getAgentId());
        if (policy.getAccountId() != agent.getAccountId()) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HOST_PARAM);
        }
        
        event.setHostId(host.getId());
        
        return super.create(type, request, next);
    }
}
