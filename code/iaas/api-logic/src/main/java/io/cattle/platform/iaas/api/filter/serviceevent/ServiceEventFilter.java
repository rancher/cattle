package io.cattle.platform.iaas.api.filter.serviceevent;


import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import javax.inject.Inject;

public class ServiceEventFilter extends AbstractDefaultResourceManagerFilter {

    public static final String VERIFY_AGENT = "CantVerifyHealthcheck";

    @Inject
    ObjectManager objectManager;

    @Inject
    AgentDao agentDao;

    @Inject
    ServiceDao serviceDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ServiceEvent.class };
    }

    protected Agent getAgent() {
        Agent agent = objectManager.loadResource(Agent.class, ApiUtils.getPolicy().getOption(Policy.AGENT_ID));
        if (agent == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        return agentDao.getHostAgentForDelegate(agent.getId());
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        ServiceEvent event = request.proxyRequestObject(ServiceEvent.class);

        /* Will never return null, MissingRequired will be thrown if missing */
        Agent agent = getAgent();
        if (agent == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        HealthcheckInstanceHostMap healthcheckInstanceHostMap = null;
        String[] splitted = event.getHealthcheckUuid().split("_");
        if (splitted.length > 2) {
            healthcheckInstanceHostMap = serviceDao.getHealthCheckInstanceUUID(splitted[0], splitted[1]);
        } else {
            healthcheckInstanceHostMap = objectManager.findOne(HealthcheckInstanceHostMap.class,
                    ObjectMetaDataManager.UUID_FIELD, splitted[0]);
        }

        if (healthcheckInstanceHostMap == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        HealthcheckInstance healthcheckInstance = objectManager.loadResource(HealthcheckInstance.class,
                healthcheckInstanceHostMap.getHealthcheckInstanceId());

        if (healthcheckInstance == null) {
            return null;
        }
        Long resourceAccId = DataAccessor.fromDataFieldOf(agent)
                .withKey(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID)
                .as(Long.class);

        if (!healthcheckInstanceHostMap.getAccountId().equals(resourceAccId)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        event.setInstanceId(healthcheckInstance.getInstanceId());
        event.setHealthcheckInstanceId(healthcheckInstance.getId());
        event.setHostId(healthcheckInstanceHostMap.getHostId());

        return super.create(type, request, next);
    }
}
