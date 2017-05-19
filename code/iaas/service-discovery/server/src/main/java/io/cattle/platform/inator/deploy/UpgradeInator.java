package io.cattle.platform.inator.deploy;

import static io.cattle.platform.core.model.tables.DeploymentUnitTable.*;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.lock.ReconcileLock;
import io.cattle.platform.inator.unit.DeploymentUnitUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;
import io.cattle.platform.lock.definition.LockDefinition;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UpgradeInator {

    ServiceWrapper service;
    InatorServices svc;
    PullInator pullInator;

    public UpgradeInator(ServiceWrapper service, InatorServices svc) {
        super();
        this.service = service;
        this.svc = svc;
        this.pullInator = new PullInator(service, svc);
    }

    public Result checkUpgrade(InatorContext context, Result result) {
        if (result.getState() == UnitState.ERROR) {
            return result;
        }

        Long batchSize = service.getBatchSize();
        Long interval = service.getInterval();
        Long lastRun = service.getLastRun();

        switch (context.getInator().getDesiredState()) {
        case INACTIVE:
            // If stopped update a lot at once
            if (batchSize < 100L) {
                batchSize = 100L;
            }
            break;
        case ACTIVE:
            break;
        default:
            return result;
        }

        Long currentRevisionId = service.getRevisionId();
        if (currentRevisionId == null) {
            return result;
        }

        Map<UnitRef, Result> results = context.getResults();
        List<DeploymentUnitUnit> currentNotGood = new ArrayList<>();
        List<DeploymentUnitUnit> toUpgrade = new ArrayList<>();
        List<DeploymentUnitUnit> upgrading = new ArrayList<>();

        context.getUnits().forEach((ref, unit) -> {
            if (unit instanceof DeploymentUnitUnit) {
                DeploymentUnitWrapper dpw = ((DeploymentUnitUnit) unit).getDeploymentUnit();
                if (dpw == null) {
                    return;
                }

                Long requestedId = dpw.getRequestRevisionId();
                long appliedId = dpw.getAppliedRevisionId();
                if (requestedId == null && appliedId == currentRevisionId.longValue()) {
                    // good, no need to upgrade. Upgrade has never happened on this unit.
                } else if (requestedId == null || !requestedId.equals(currentRevisionId)) {
                    toUpgrade.add((DeploymentUnitUnit) unit);
                } else if (appliedId != requestedId.longValue()) {
                    upgrading.add((DeploymentUnitUnit) unit);
                } else if (!results.containsKey(unit.getRef()) || !results.get(unit.getRef()).isGood()) {
                    currentNotGood.add((DeploymentUnitUnit) unit);
                }
            }
        });

        if (toUpgrade.size() > 0 || upgrading.size() > 0) {
            Result pullState = pullInator.pull();
            if (!pullState.isGood()) {
                result.aggregate(pullState);
                return result;
            }

            if (upgrading.size() != 0) {
                result.aggregate(new Result(UnitState.WAITING, null, "Waiting for current units to upgrade"));
                return result;
            }

            if (currentNotGood.size() != 0) {
                result.aggregate(new Result(UnitState.WAITING, null, "Waiting for current upgraded units to be healthy"));
                return result;
            }

            if (System.currentTimeMillis() <= (lastRun + interval)) {
                throw new ProcessDelayException(new Date(lastRun + interval));
            }

            boolean scheduled = false;
            long count = Math.min(toUpgrade.size(), batchSize);
            for (int i = 0 ; i < count ; i++) {
                if (i >= toUpgrade.size()) {
                    break;
                }

                DeploymentUnitUnit deploymentUnit = toUpgrade.get(i);
                Result upgradeResult = new Result(UnitState.WAITING, deploymentUnit, String.format("Upgrading %s", deploymentUnit.getDisplayName()));
                result.aggregate(upgradeResult);
                upgrade(deploymentUnit, currentRevisionId);
                scheduled = true;
            }

            service.setUpgradeLastRunToNow();
            if (scheduled) {
                throw new ProcessDelayException(new Date(System.currentTimeMillis() + interval));
            }
        }

        return result;
    }

    private boolean upgrade(DeploymentUnitUnit deploymentUnit, Long currentRevisionId) {
        LockDefinition lockDef = new ReconcileLock(ServiceConstants.KIND_DEPLOYMENT_UNIT, deploymentUnit.getDeploymentUnit().getId());
        return svc.lockManager.tryLock(lockDef, () -> {
            DeploymentUnit unit = svc.objectManager.loadResource(DeploymentUnit.class, deploymentUnit.getDeploymentUnit().getId());

            svc.objectManager.setFields(unit,
                    DEPLOYMENT_UNIT.REQUESTED_REVISION_ID, currentRevisionId);
            svc.triggerDeploymentUnitReconcile(unit.getId());

            return true;
        }) != null;
    }

}