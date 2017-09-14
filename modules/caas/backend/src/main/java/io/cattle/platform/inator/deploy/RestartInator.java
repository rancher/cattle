package io.cattle.platform.inator.deploy;

import io.cattle.platform.engine.process.impl.ProcessDelayException;
import io.cattle.platform.inator.Inator.DesiredState;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.unit.DeploymentUnitUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RestartInator {

    ServiceWrapper service;
    InatorServices svc;
    PullInator pullInator;

    public RestartInator(ServiceWrapper service, InatorServices svc) {
        super();
        this.service = service;
        this.svc = svc;
        this.pullInator = new PullInator(service, svc);
    }

    public Result checkRestart(InatorContext context, Result result) {
        if (!result.isGood() || context.getInator().getDesiredState() != DesiredState.ACTIVE) {
            return result;
        }

        Long batchSize = service.getBatchSize();
        Long interval = service.getInterval();
        Long lastRun = service.getLastRun();
        Long restartTrigger = service.getRestartTrigger();

        List<DeploymentUnitUnit> toRestart = new ArrayList<>();

        context.getUnits().forEach((ref, unit) -> {
            if (unit instanceof DeploymentUnitUnit) {
                DeploymentUnitWrapper dpw = ((DeploymentUnitUnit) unit).getDeploymentUnit();
                if (dpw == null) {
                    return;
                }
                Long duRestartTrigger = dpw.getRestartTrigger();

                if (duRestartTrigger < restartTrigger) {
                    toRestart.add((DeploymentUnitUnit) unit);
                }
            }
        });

        if (toRestart.size() > 0) {
            if (System.currentTimeMillis() <= (lastRun + interval)) {
                throw new ProcessDelayException(new Date(lastRun + interval));
            }

            boolean scheduled = false;
            long count = Math.min(toRestart.size(), batchSize);
            for (int i = 0 ; i < count ; i++) {
                if (i >= toRestart.size()) {
                    break;
                }

                DeploymentUnitUnit deploymentUnit = toRestart.get(i);
                Result upgradeResult = new Result(UnitState.WAITING, deploymentUnit, String.format("Restarting %s", deploymentUnit.getDisplayName()));
                result.aggregate(upgradeResult);
                svc.processManager.deactivate(deploymentUnit.getDeploymentUnit().getInternal(), null);
                scheduled = true;
            }

            service.setUpgradeLastRunToNow();
            if (scheduled) {
                throw new ProcessDelayException(new Date(System.currentTimeMillis() + interval));
            }
        }

        return result;
    }

}