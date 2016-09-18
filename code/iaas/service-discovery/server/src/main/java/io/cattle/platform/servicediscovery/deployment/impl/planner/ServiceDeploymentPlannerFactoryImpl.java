package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;


public class ServiceDeploymentPlannerFactoryImpl implements ServiceDeploymentPlannerFactory {

    @Override
    public ServiceDeploymentPlanner createServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units,
            DeploymentServiceContext context) {

        if (services.isEmpty()) {
            return null;
        }

        Service service = services.get(0);
        boolean isGlobalDeploymentStrategy = isGlobalDeploymentStrategy(context, service);
        boolean isSelectorOnlyStrategy = isNoopStrategy(context, service);
        if (isSelectorOnlyStrategy
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_EXTERNAL_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_DNS_SERVICE)) {
            return new NoOpServiceDeploymentPlanner(services, units, context);
        } else if (isGlobalDeploymentStrategy) {
            return new GlobalServiceDeploymentPlanner(services, units, context);
        } else {
            return new DefaultServiceDeploymentPlanner(services, units, context);
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
        if (service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_DNS_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_EXTERNAL_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_LOAD_BALANCER_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND_SERVICE)) {
            return false;
        }
        return true;
    }
}
