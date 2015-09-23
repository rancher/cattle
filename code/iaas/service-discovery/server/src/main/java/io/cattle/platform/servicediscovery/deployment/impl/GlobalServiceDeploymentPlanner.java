package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalServiceDeploymentPlanner extends ServiceDeploymentPlanner {
    List<Long> hostIds = new ArrayList<>();
    Map<Long, DeploymentUnit> hostToUnits = new HashMap<>();

    public GlobalServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        super(services, units, context);
        // TODO: Do we really need to iterate or is there just one service that we're dealing with here?
        for (Service service : services) {
            List<Long> hostIdsToDeployService =
                    context.allocatorService.getHostsSatisfyingHostAffinity(service.getAccountId(),
                            ServiceDiscoveryUtil.getServiceLabelsUnion(service, context.allocatorService));
            hostIds.addAll(hostIdsToDeployService);
        }
        for (DeploymentUnit unit : units) {
            Map<String, String> unitLabels = unit.getLabels();
            String hostId = unitLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
            hostToUnits.put(Long.valueOf(hostId), unit);
        }
    }

    @Override
    public List<DeploymentUnit> deployHealthyUnits() {
        if (this.healthyUnits.size() < hostIds.size()) {
            addMissingUnits();
        }
        return healthyUnits;
    }

    private void addMissingUnits() {
        for (Long hostId : hostIds) {
            if (!hostToUnits.containsKey(hostId)) {
                Map<String, String> labels = new HashMap<>();
                labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID, hostId.toString());
                DeploymentUnit unit = new DeploymentUnit(context, services, labels);
                hostToUnits.put(hostId, unit);
                healthyUnits.add(unit);
            }
        }
    }

    @Override
    public boolean needToReconcileDeploymentImpl() {
        return (healthyUnits.size() != hostIds.size());
    }
}
