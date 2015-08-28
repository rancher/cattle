package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlannerFactory;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

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
        boolean isSelectorOnlyStrategy = isSelectorOnlyStrategy(context, service);
        if (isSelectorOnlyStrategy
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.EXTERNALSERVICE.name())
                || service.getKind().equalsIgnoreCase(ServiceDiscoveryConstants.KIND.DNSSERVICE.name())) {
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
        return globalService != null;
    }

    protected boolean isSelectorOnlyStrategy(DeploymentServiceContext context, Service service) {
        boolean selectorOnly = false;
        Object imageUUID = ServiceDiscoveryUtil.getServiceDataAsMap(service,
                ServiceDiscoveryConstants.PRIMARY_LAUNCH_CONFIG_NAME,
                context.allocatorService).get(InstanceConstants.FIELD_IMAGE_UUID);
        if (service.getSelectorContainer() != null
                && (imageUUID == null || imageUUID.toString().equalsIgnoreCase(ServiceDiscoveryConstants.IMAGE_NONE))) {
            selectorOnly = true;
        }
        return selectorOnly;
    }
}
