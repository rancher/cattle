package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
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

    public GlobalServiceDeploymentPlanner(Service service, List<DeploymentUnit> units,
            DeploymentServiceContext context) {
        super(service, units, context);
        // TODO: Do we really need to iterate or is there just one service that we're dealing with here?
        List<Long> hostIdsToDeployService =
                context.allocatorService.getHostsSatisfyingHostAffinity(service.getAccountId(),
                        ServiceDiscoveryUtil.getMergedServiceLabels(service, context.allocatorService));
        hostIds.addAll(hostIdsToDeployService);
        for (DeploymentUnit unit : units) {
            Map<String, String> unitLabels = unit.getLabels();
            String hostId = unitLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
            hostToUnits.put(Long.valueOf(hostId), unit);
        }
    }

    @Override
    public List<DeploymentUnit> deployHealthyUnits() {
        // add missing units
        if (needToReconcileDeploymentImpl()) {
            addMissingUnits();
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

        List<String> hostIds = new ArrayList<>();
        for (int i = 0; i < this.healthyUnits.size(); i++) {
            DeploymentUnit unit = this.healthyUnits.get(i);
            Map<String, String> unitLabels = unit.getLabels();
            String hostId = unitLabels.get(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
            if (hostIds.contains(hostId)) {
                watchList.add(unit);
                unit.remove(false, ServiceDiscoveryConstants.AUDIT_LOG_REMOVE_EXTRA, ActivityLog.INFO);
                this.healthyUnits.remove(i);
            } else {
                hostIds.add(hostId);
            }
        }

        for (DeploymentUnit toWatch : watchList) {
            toWatch.waitForRemoval();
        }
    }

    private void addMissingUnits() {
        for (Long hostId : hostIds) {
            if (!hostToUnits.containsKey(hostId)) {
                Map<String, String> labels = new HashMap<>();
                labels.put(ServiceDiscoveryConstants.LABEL_SERVICE_REQUESTED_HOST_ID, hostId.toString());
                DeploymentUnit unit = new DeploymentUnit(context, service, labels);
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
