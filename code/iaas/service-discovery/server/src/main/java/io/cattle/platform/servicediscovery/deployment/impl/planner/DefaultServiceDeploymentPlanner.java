package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.service.impl.DeploymentManagerImpl.DeploymentManagerContext;
import io.cattle.platform.util.exception.ServiceReconcileException;

import java.util.ArrayList;
import java.util.List;

public class DefaultServiceDeploymentPlanner extends AbstractServiceDeploymentPlanner {

    protected int requestedScale = 0;

    public DefaultServiceDeploymentPlanner(Service service, Stack stack,
            DeploymentManagerContext context) {
        super(service, context, stack);
        // internal desired scale populated by scale policy driven deployment
        int scale = DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_SCALE);

        if (scale > this.requestedScale) {
            this.requestedScale = scale;
        }
    }

    @Override
    protected void checkScale() {
        int scale = DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_SCALE);
        if (scale != requestedScale) {
            throw new ServiceReconcileException("Need to restart service reconcile");
        }
    }

    @Override
    public List<DeploymentUnit> reconcileUnitsList(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        if (getAllUnits().size() < requestedScale) {
            addMissingUnits(svcInstanceIdGenerator);
        } else if (getAllUnits().size() > requestedScale) {
            removeExtraUnits();
        }

        return getAllUnitsList();
    }

    private void addMissingUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        while (getAllUnits().size() < this.requestedScale) {
            DeploymentUnit unit = context.serviceDao.createDeploymentUnit(service.getAccountId(), service, null,
                    svcInstanceIdGenerator.getNextAvailableId());
            addUnit(unit, State.HEALTHY);
        }
    }

    private void removeExtraUnits() {
        List<DeploymentUnit> units = getAllUnitsList();
        if (units.size() == 0) {
            return;
        }
        // delete units
        int i = units.size() - 1;
        sortByCreated(units);
        List<DeploymentUnit> watchList = new ArrayList<>();
        while (units.size() > this.requestedScale) {
            DeploymentUnit toRemove = units.get(i);
            watchList.add(toRemove);
            removeUnit(toRemove, State.EXTRA, ServiceConstants.AUDIT_LOG_REMOVE_EXTRA, ActivityLog.INFO);
            units.remove(i);
            i--;
        }
        for (DeploymentUnit toWatch : watchList) {
            waitForRemoval(toWatch);
        }
    }

    @Override
    public boolean needToReconcileScale() {
        return (getAllUnits().size() != requestedScale);
    }

    @Override
    public String getStatus() {
        return String.format("Requested: %d, %s", requestedScale, super.getStatus());
    }
}
