package io.cattle.platform.servicediscovery.deployment.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class GlobalServiceDeploymentPlanner extends ServiceDeploymentPlanner {
    List<Long> hostIds = new ArrayList<>();
    Map<Long, DeploymentUnit> hostToUnits = new HashMap<>();

    public GlobalServiceDeploymentPlanner(List<Service> services, List<DeploymentUnit> units, DeploymentServiceContext context) {
        super(services, units, context);
        Map<String, String> globalLabels = new HashMap<>();
        globalLabels.put(ServiceDiscoveryConstants.LABEL_SERVICE_GLOBAL, null);
        this.hostIds = context.allocatorService.getHostsForGlobalService(services.get(0).getAccountId(), globalLabels);
        for (DeploymentUnit unit : units) {
            Map<String, String> unitLables = unit.getLabels();
            String hostId = unitLables.get(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
            hostToUnits.put(Long.valueOf(hostId), unit);
        }
    }

    @Override
    public List<DeploymentUnit> deployHealthyUnits() {
        if (this.healthyUnits.size() < hostIds.size()) {
            addMissingUnits();
        } else if (healthyUnits.size() > hostIds.size()) {
            removeExtraUnits();
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

    private void removeExtraUnits() {
        for (Long hostId : hostToUnits.keySet()) {
            if (!hostIds.contains(hostId)) {
                DeploymentUnit toRemove = hostToUnits.get(hostId);
                toRemove.remove();
                Iterator<DeploymentUnit> iter = this.healthyUnits.iterator();
                while (iter.hasNext()) {
                    DeploymentUnit unit = iter.next();
                    if (unit.getUuid().equals(toRemove.uuid)) {
                        iter.remove();
                        break;
                    }
                }
                hostToUnits.remove(hostId);
            }
        }
    }

    @Override
    public boolean needToReconcileDeployment() {
        return (healthyUnits.size() != hostIds.size());
    }
}
