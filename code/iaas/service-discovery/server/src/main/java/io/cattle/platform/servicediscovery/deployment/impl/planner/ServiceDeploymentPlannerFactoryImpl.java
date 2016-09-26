package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;


public class ServiceDeploymentPlannerFactoryImpl implements ServiceDeploymentPlannerFactory {

    @Override
    public ServiceDeploymentPlanner createServiceDeploymentPlanner(Service service, List<DeploymentUnit> units,
            DeploymentServiceContext context) {

        if (service == null) {
            return null;
        }

        boolean isGlobalDeploymentStrategy = isGlobalDeploymentStrategy(context, service);
        boolean isSelectorOnlyStrategy = isNoopStrategy(context, service);
        if (isSelectorOnlyStrategy
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)) {
            return new NoOpServiceDeploymentPlanner(service, units, context);
        } else if (isGlobalDeploymentStrategy) {
            return new GlobalServiceDeploymentPlanner(service, units, context);
        } else {
            return new DefaultServiceDeploymentPlanner(service, units, context);
        }
    }

    protected boolean isGlobalDeploymentStrategy(DeploymentServiceContext context, Service service) {
        return context.sdService.isGlobalService(service);
    }

    protected boolean isNoopStrategy(DeploymentServiceContext context, Service service) {
        if (ServiceDiscoveryUtil.isNoopService(service) || isExternallyProvidedService(service)) {
            return true;
        }
        return false;
    }

    protected boolean isExternallyProvidedService(Service service) {
        if (service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_LOAD_BALANCER_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_SERVICE)) {
            return false;
        }
        return true;
    }
}
