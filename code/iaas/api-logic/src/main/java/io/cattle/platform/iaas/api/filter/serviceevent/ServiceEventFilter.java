package io.cattle.platform.iaas.api.filter.serviceevent;


import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class ServiceEventFilter extends AbstractDefaultResourceManagerFilter {

    private static final long STAY_ACTIVE_MILLISECONDS = ArchaiusUtil.getLong("stay_active.milliseconds").getValue();

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

        if (!isNetworkUp(resourceAccId)) {
            throw new ClientVisibleException(ResponseCodes.CONFLICT);
        }

        event.setInstanceId(healthcheckInstance.getInstanceId());
        event.setHealthcheckInstanceId(healthcheckInstance.getId());
        event.setHostId(healthcheckInstanceHostMap.getHostId());

        return super.create(type, request, next);
    }

    private boolean isNetworkUp(long accountId) {
        Service networkDriverService = objectManager.findAny(Service.class, SERVICE.ACCOUNT_ID, accountId, SERVICE.REMOVED, null, SERVICE.KIND,
                ServiceConstants.KIND_NETWORK_DRIVER_SERVICE);
        if (networkDriverService == null) {
            return true;
        }
        List<Service> services = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, accountId, SERVICE.REMOVED, null, SERVICE.STACK_ID,
                networkDriverService.getStackId());
        for (Service service : services) {
            if (!service.getState().equals(CommonStatesConstants.ACTIVE)) {
                return false;
            }
            Object obj = DataAccessor.fields(service).withKey(ServiceConstants.LAST_ACTIVE).get();
            if(obj != null) {
                long lastActiveTime = (long) obj;
                long currentTime = new Date().getTime();
                // networkUp only if service has been active for STAY_ACTIVE_SECONDS
                if (lastActiveTime + STAY_ACTIVE_MILLISECONDS > currentTime) {
                    return false;
                }
            }
        }
        return true;
    }
}
