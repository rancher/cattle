package io.cattle.platform.inator.deploy;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.planner.UnitPlanner;
import io.cattle.platform.inator.unit.DeploymentUnitUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceInator implements Inator {

    ServiceWrapper service;
    Set<UnitRef> desiredSet;
    InatorServices svc;
    UnitPlanner planner;
    UpgradeInator upgrade;
    RestartInator restart;
    ProcessKickamajig processKicker;

    public ServiceInator(Service service, UnitPlanner planner, InatorServices svc) {
        super();
        this.service = new ServiceWrapper(service, svc);
        this.svc = svc;
        this.planner = planner;
        this.upgrade = new UpgradeInator(this.service, svc);
        this.restart = new RestartInator(this.service, svc);
        this.processKicker = new ProcessKickamajig(svc);
    }

    @Override
    public List<Unit> collect() {
        return svc.serviceDao.getDeploymentUnits(service.getService()).values().stream()
            .map((deploymentUnit) -> new DeploymentUnitUnit(new DeploymentUnitWrapper(deploymentUnit, service.getService(), svc), service, svc))
            .collect(Collectors.toList());
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        return planner.fillIn(context);
    }

    @Override
    public Set<UnitRef> getDesiredRefs() {
        return planner.getDesiredUnits();
    }

    @Override
    public DesiredState getDesiredState() {
        return service.getDesiredState();
    }

    @Override
    public Result postProcess(InatorContext context, Result result) {
        result = upgrade.checkUpgrade(context, result);
        result = restart.checkRestart(context, result);
        processKicker.kickProcess(service.getInternal(), service.getState(), result.getState());
        return result;
    }

}