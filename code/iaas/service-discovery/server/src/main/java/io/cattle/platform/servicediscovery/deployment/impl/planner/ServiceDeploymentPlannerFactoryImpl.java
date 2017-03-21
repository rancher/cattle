package io.cattle.platform.servicediscovery.deployment.impl.planner;

import static io.cattle.platform.core.model.tables.StackTable.*;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
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
        Stack stack = context.objectManager.findOne(Stack.class, STACK.ID, service.getStackId());

        boolean isGlobalDeploymentStrategy = isGlobalDeploymentStrategy(context, service);
        boolean isSelectorOnlyStrategy = isNoopStrategy(context, service);
        if (isSelectorOnlyStrategy
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_EXTERNAL_SERVICE)
                || service.getKind().equalsIgnoreCase(ServiceConstants.KIND_DNS_SERVICE)) {
            return new NoOpServiceDeploymentPlanner(service, stack, units, context);
        } else if (isGlobalDeploymentStrategy) {
            return new GlobalServiceDeploymentPlanner(service, stack, units, context);
        } else {
            return new DefaultServiceDeploymentPlanner(service, stack, units, context);
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
                || ServiceConstants.SERVICE_LIKE.contains(service.getKind())) {
            return false;
        }
        return true;
    }
}
