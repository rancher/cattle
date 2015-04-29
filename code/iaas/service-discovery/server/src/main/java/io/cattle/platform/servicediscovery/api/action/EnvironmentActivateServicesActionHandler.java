package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.process.ServiceUpdateActivate;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class EnvironmentActivateServicesActionHandler implements ActionHandler {

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return ServiceDiscoveryConstants.PROCESS_ENV_ACTIVATE_SERVICES;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Environment)) {
            return null;
        }
        Environment env = (Environment) obj;
        List<? extends Service> services = objectManager.mappedChildren(env, Service.class);
        activateServices(services, new HashMap<String, Object>());

        return env;
    }

    private void activateServices(List<? extends Service> services, Map<String, Object> data) {
        // flag for service.activate to indicate that the consumed services should be activated first
        DataAccessor.fromMap(data).withScope(ServiceUpdateActivate.class)
                .withKey(ServiceDiscoveryConstants.FIELD_ACTIVATE_CONSUMED_SERVICES).set(true);
        for (Service service : services) {
            if (service.getState().equalsIgnoreCase(CommonStatesConstants.INACTIVE)) {
                objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_ACTIVATE,
                            service, data);
            }
        }
    }
    
}
