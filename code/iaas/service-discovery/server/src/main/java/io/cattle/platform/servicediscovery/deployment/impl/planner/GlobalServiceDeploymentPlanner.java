package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.util.ServiceUtil;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.service.impl.DeploymentManagerImpl.DeploymentManagerContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalServiceDeploymentPlanner extends AbstractServiceDeploymentPlanner {
    
    List<Long> hostIds = new ArrayList<>();
    Map<Long, DeploymentUnit> hostToUnits = new HashMap<>();

    public GlobalServiceDeploymentPlanner(Service service, Stack stack, DeploymentManagerContext context) {
        super(service, context, stack);
        Map<String, String> labels = ServiceUtil.getMergedServiceLabels(service);
        if (service.getSystem()) {
            labels.put(SystemLabels.LABEL_CONTAINER_SYSTEM, "true");
        }
        List<Long> hostIdsToDeployService =
                context.allocationHelper.getHostsSatisfyingHostAffinity(service.getAccountId(), labels);
        hostIds.addAll(hostIdsToDeployService);
        ignoreUnits();
        for (DeploymentUnit unit : this.getAllUnits().values()) {
            hostToUnits.put(Long.valueOf(getHostId(unit)), unit);
        }
    }

    public void ignoreUnits() {
        List<DeploymentUnit> units = getAllUnitsList();
        for (DeploymentUnit unit : units) {
            if (!hostIds.contains(Long.valueOf(getHostId(unit)))) {
                removeFromList(unit, State.EXTRA);
            }
        }
    }

    @Override
    public List<DeploymentUnit> reconcileUnitsList(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        // add missing units
        if (needToReconcileScale()) {
            addMissingUnits(svcInstanceIdGenerator);
        }
        // remove extra units
        removeExtraUnits();

        return getAllUnitsList();
    }

    private void removeExtraUnits() {
        List<DeploymentUnit> units = getAllUnitsList();
        if (units.size() == 0) {
            return;
        }

        // delete units
        sortByCreated(units);
        List<String> fulfilledHostIds = new ArrayList<>();
        List<DeploymentUnit> watchList = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            DeploymentUnit unit = units.get(i);
            String hostId = getHostId(unit);
            if (fulfilledHostIds.contains(hostId) || !hostIds.contains(Long.valueOf(hostId))) {
                watchList.add(unit);
                removeUnit(unit, State.EXTRA, ServiceConstants.AUDIT_LOG_REMOVE_EXTRA, ActivityLog.INFO);
                units.remove(i);
            } else {
                fulfilledHostIds.add(hostId);
            }
        }
        for (DeploymentUnit toWatch : watchList) {
            waitForRemoval(toWatch);
        }
    }

    private void addMissingUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        for (Long hostId : hostIds) {
            if (!hostToUnits.containsKey(hostId)) {
                Map<String, String> labels = new HashMap<>();
                labels.put(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID, hostId.toString());
                DeploymentUnit unit = context.serviceDao.createDeploymentUnit(service.getAccountId(), service, labels,
                        svcInstanceIdGenerator.getNextAvailableId());
                hostToUnits.put(hostId, unit);
                addUnit(unit, State.HEALTHY);
            }
        }
    }

    @Override
    public boolean needToReconcileScale() {
        return !hostToUnits.keySet().containsAll(hostIds);
    }

    @SuppressWarnings("unchecked")
    private String getHostId(DeploymentUnit unit) {
        Map<String, String> unitLabels = DataAccessor.fields(unit).withKey(InstanceConstants.FIELD_LABELS)
                .as(Map.class);
        String hostId = unitLabels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        return hostId;
    }

    @Override
    protected void checkScale() {
        return;
    }
}
