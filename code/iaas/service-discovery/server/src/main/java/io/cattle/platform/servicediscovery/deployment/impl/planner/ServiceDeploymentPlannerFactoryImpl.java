package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.List;
import java.util.Map;


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
        Map<String, String> serviceLabels = ServiceDiscoveryUtil.getServiceLabels(service, context.allocatorService);
        String globalService = serviceLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_GLOBAL);
        return Boolean.valueOf(globalService);
    }

    protected boolean isNoopStrategy(DeploymentServiceContext context, Service service) {
        if (ServiceDiscoveryUtil.isNoopService(service, context.allocatorService) || isExternallyProvidedService(service)) {
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
