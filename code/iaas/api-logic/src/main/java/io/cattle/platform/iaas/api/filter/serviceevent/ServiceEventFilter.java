package io.cattle.platform.iaas.api.filter.serviceevent;


import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

public class ServiceEventFilter extends AbstractValidationFilter {

    public static final String VERIFY_AGENT = "CantVerifyHealthcheck";

    ObjectManager objectManager;
    AgentDao agentDao;
    ServiceDao serviceDao;

    public ServiceEventFilter(ObjectManager objectManager, AgentDao agentDao, ServiceDao serviceDao) {
        super();
        this.objectManager = objectManager;
        this.agentDao = agentDao;
        this.serviceDao = serviceDao;
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
        Long resourceAccId = agent.getResourceAccountId();

        if (!healthcheckInstanceHostMap.getAccountId().equals(resourceAccId)) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }

        event.setInstanceId(healthcheckInstance.getInstanceId());
        event.setHealthcheckInstanceId(healthcheckInstance.getId());
        event.setHostId(healthcheckInstanceHostMap.getHostId());

        return super.create(type, request, next);
    }
}
