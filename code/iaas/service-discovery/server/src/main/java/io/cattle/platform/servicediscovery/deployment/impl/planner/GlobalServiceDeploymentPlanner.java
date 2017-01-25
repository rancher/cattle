package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalServiceDeploymentPlanner extends ServiceDeploymentPlanner {
    List<Long> hostIds = new ArrayList<>();
    Map<Long, DeploymentUnit> hostToUnits = new HashMap<>();

    public GlobalServiceDeploymentPlanner(Service service, Stack stack,
            List<DeploymentUnit> units, DeploymentServiceContext context) {
        super(service, units, context, stack);
        List<Long> hostIdsToDeployService =
                context.allocatorService.getHostsSatisfyingHostAffinity(service.getAccountId(),
                        ServiceDiscoveryUtil.getMergedServiceLabels(service, context.allocatorService));
        hostIds.addAll(hostIdsToDeployService);
        for (DeploymentUnit unit : units) {
            hostToUnits.put(Long.valueOf(getHostId(unit)), unit);
        }
    }


    @Override
    public List<DeploymentUnit> deployHealthyUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        // add missing units
        if (needToReconcileDeploymentImpl()) {
            addMissingUnits(svcInstanceIdGenerator);
        }
        // remove extra units
        removeExtraUnits();

        return healthyUnits;
    }

    private void removeExtraUnits() {
        // delete units
        List<DeploymentUnit> watchList = new ArrayList<>();
        Collections.sort(this.healthyUnits, new Comparator<DeploymentUnit>() {
            @Override
            public int compare(DeploymentUnit d1, DeploymentUnit d2) {
                return Long.compare(d1.getCreateIndex(), d2.getCreateIndex());
            }
        });

        Collections.sort(this.incompleteUnits, new Comparator<DeploymentUnit>() {
            @Override
            public int compare(DeploymentUnit d1, DeploymentUnit d2) {
                return Long.compare(d1.getCreateIndex(), d2.getCreateIndex());
            }
        });

        List<String> fulfilledHostIds = new ArrayList<>();
        for (int i = 0; i < this.healthyUnits.size(); i++) {
            DeploymentUnit unit = this.healthyUnits.get(i);
            String hostId = getHostId(unit);
            boolean hostPresent = hostIds.contains(Long.valueOf(hostId));
            if (fulfilledHostIds.contains(hostId) || !hostPresent) {
                // remove when host is present, just ignore the unit if not (by removing from healthy units)
                if (hostPresent) {
                    watchList.add(unit);
                    unit.remove(ServiceConstants.AUDIT_LOG_REMOVE_EXTRA, ActivityLog.INFO);
                }
                this.healthyUnits.remove(i);
            } else {
                fulfilledHostIds.add(hostId);
            }
        }

        for (DeploymentUnit toWatch : watchList) {
            toWatch.waitForRemoval();
        }

        for (int i = 0; i < this.incompleteUnits.size(); i++) {
            DeploymentUnit unit = this.incompleteUnits.get(i);
            String hostId = getHostId(unit);
            boolean hostPresent = hostIds.contains(Long.valueOf(hostId));
            if (!hostPresent) {
                this.incompleteUnits.remove(i);
            }
        }
    }

    private String getHostId(DeploymentUnit unit) {
        Map<String, String> unitLabels = unit.getLabels();
        String hostId = unitLabels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        return hostId;
    }


    private void addMissingUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        for (Long hostId : hostIds) {
            if (!hostToUnits.containsKey(hostId)) {
                Map<String, String> labels = new HashMap<>();
                labels.put(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID, hostId.toString());
                DeploymentUnit unit = new DeploymentUnit(context, service, labels, svcInstanceIdGenerator, stack);
                hostToUnits.put(hostId, unit);
                healthyUnits.add(unit);
            }
        }
    }

    @Override
    public boolean needToReconcileDeploymentImpl() {
        return !hostToUnits.keySet().containsAll(hostIds);
    }
}
