package io.cattle.platform.servicediscovery.deployment.impl.planner;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.deployment.DeploymentUnitInstanceIdGenerator;
import io.cattle.platform.servicediscovery.deployment.ServiceDeploymentPlanner;
import io.cattle.platform.servicediscovery.deployment.impl.DeploymentManagerImpl.DeploymentServiceContext;
import io.cattle.platform.servicediscovery.deployment.impl.unit.DeploymentUnit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DefaultServiceDeploymentPlanner extends ServiceDeploymentPlanner {

    protected Integer requestedScale = 0;

    public DefaultServiceDeploymentPlanner(Service service, Stack stack,
            List<DeploymentUnit> units, DeploymentServiceContext context) {
        super(service, units, context, stack);
        int scale;
        // internal desired scale populated by scale policy driven deployment
        Integer scaleInternal = DataAccessor.fieldInteger(service,
                ServiceConstants.FIELD_DESIRED_SCALE);
        if (scaleInternal != null) {
            scale = scaleInternal;
        } else {
            scale = DataAccessor.fieldInteger(service,
                    ServiceConstants.FIELD_SCALE);
        }

        if (scale > this.requestedScale) {
            this.requestedScale = scale;
        }
    }

    @Override
    public List<DeploymentUnit> deployHealthyUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        if (this.healthyUnits.size() < requestedScale) {
            addMissingUnits(svcInstanceIdGenerator);
        } else if (healthyUnits.size() > requestedScale) {
            removeExtraUnits();
        }

        return healthyUnits;
    }

    private void addMissingUnits(DeploymentUnitInstanceIdGenerator svcInstanceIdGenerator) {
        while (this.healthyUnits.size() < this.requestedScale) {
            DeploymentUnit unit = new DeploymentUnit(context, service, null, svcInstanceIdGenerator, stack);
            this.healthyUnits.add(unit);
        }
    }

    private void removeExtraUnits() {
        // delete units
        int i = this.healthyUnits.size() - 1;
        List<DeploymentUnit> watchList = new ArrayList<>();
        Collections.sort(this.healthyUnits, new Comparator<DeploymentUnit>() {
            @Override
            public int compare(DeploymentUnit d1, DeploymentUnit d2) {
                return Long.compare(d1.getCreateIndex(), d2.getCreateIndex());
            }
        });

        while (this.healthyUnits.size() > this.requestedScale) {
            DeploymentUnit toRemove = this.healthyUnits.get(i);
            watchList.add(toRemove);
            toRemove.remove(ServiceConstants.AUDIT_LOG_REMOVE_EXTRA, ActivityLog.INFO);
            this.healthyUnits.remove(i);
            i--;
        }

        for (DeploymentUnit toWatch : watchList) {
            toWatch.waitForRemoval();
        }
    }

    @Override
    public boolean needToReconcileDeploymentImpl() {
        return (healthyUnits.size() != requestedScale);
    }

    @Override
    public String getStatus() {
        return String.format("Requested: %d, %s", requestedScale, super.getStatus());
    }
}
