package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class EnvironmentDeactivateServicesActionHandler implements ActionHandler {

    @Inject
    ObjectProcessManager objectProcessManager;

    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return ServiceDiscoveryConstants.PROCESS_ENV_DEACTIVATE_SERVICES;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Environment)) {
            return null;
        }
        Environment env = (Environment) obj;
        List<? extends Service> services = objectManager.mappedChildren(env, Service.class);
        deactivateServices(services);

        return env;
    }

    private void deactivateServices(List<? extends Service> services) {
        List<String> validStates = Arrays.asList(CommonStatesConstants.ACTIVE, CommonStatesConstants.ACTIVATING,
                CommonStatesConstants.UPDATING_ACTIVE, CommonStatesConstants.UPDATING_INACTIVE,
                ServiceDiscoveryConstants.STATE_RESTARTING, CommonStatesConstants.DEACTIVATING);
        for (Service service : services) {
            if (validStates.contains(service.getState())) {
                if (!service.getState().equals(CommonStatesConstants.DEACTIVATING)) {
                    objectProcessManager.scheduleProcessInstance(ServiceDiscoveryConstants.PROCESS_SERVICE_DEACTIVATE,
                            service, null);
                }
            } else {
                throw new ValidationErrorException(ValidationErrorCodes.INVALID_STATE,
                        "Service " + service.getName() + " is not in valid state to be deactivated: "
                                + service.getState());
            }
        }
    }
}