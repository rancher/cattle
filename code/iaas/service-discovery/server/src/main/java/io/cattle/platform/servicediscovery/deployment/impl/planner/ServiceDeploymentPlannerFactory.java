package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.service.impl.DeploymentManagerImpl.DeploymentManagerContext;

public class ServiceDeploymentPlannerFactory {
    public static ServiceDeploymentPlanner getServiceDeploymentPlanner(Service service, Stack stack,
            DeploymentManagerContext context) {
        if (service == null) {
            return null;
        }
        boolean isGlobalDeploymentStrategy = ServiceUtil.isGlobalService(service);
        boolean isSelectorOnlyStrategy = isNoopStrategy(service);
        if (isSelectorOnlyStrategy
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)) {
            return new NoOpServiceDeploymentPlanner(service, stack, context);
        } else if (isGlobalDeploymentStrategy) {
            return new GlobalServiceDeploymentPlanner(service, stack, context);
        } else {
            return new DefaultServiceDeploymentPlanner(service, stack, context);
        }
    }

    protected static boolean isNoopStrategy(Service service) {
        if (ServiceUtil.isNoopService(service) || isExternallyProvidedService(service)) {
            return true;
        }
        return false;
    }

    protected static boolean isExternallyProvidedService(Service service) {
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                || ServiceConstants.SERVICE_LIKE.contains(service.getKind())) {
            return false;
        }
        return true;
    }
}
