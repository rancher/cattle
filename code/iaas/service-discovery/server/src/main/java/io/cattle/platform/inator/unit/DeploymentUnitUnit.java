package io.cattle.platform.inator.unit;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Inator.DesiredState;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.PausableUnit;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.Collection;
import java.util.Collections;

public class DeploymentUnitUnit implements Unit, BasicStateUnit, PausableUnit {

    DeploymentUnitWrapper unit;
    ServiceWrapper service;
    UnitRef ref;
    String index;
    Long hostId;
    InatorServices svc;

    public DeploymentUnitUnit(DeploymentUnitWrapper unit, ServiceWrapper service, InatorServices svc) {
        this.service = service;
        this.unit = unit;
        this.ref = this.unit.getRef();
        this.svc = svc;
    }

    public DeploymentUnitUnit(ServiceWrapper service, long hostId, String index, InatorServices svc) {
        this.service = service;
        this.ref = DeploymentUnitWrapper.newRef(hostId, null, svc);
        this.hostId = hostId;
        this.index = index;
        this.service = service;
        this.svc = svc;
    }

    public DeploymentUnitUnit(ServiceWrapper service, String index, InatorServices svc) {
        this.service = service;
        this.ref = DeploymentUnitWrapper.newRef(null, index, svc);
        this.index = index;
        this.service = service;
        this.svc = svc;
    }

    public DeploymentUnitWrapper getDeploymentUnit() {
        return unit;
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        if (unit != null || !desired) {
            return Result.good();
        }

        long start = System.currentTimeMillis();
        Service service = this.service.getService();
        DeploymentUnit unit = svc.serviceDao.createDeploymentUnit(service.getAccountId(), service.getId(),
                service.getStackId(), hostId, index, service.getRevisionId(), context.getInator().getDesiredState() == DesiredState.ACTIVE);
        this.unit = new DeploymentUnitWrapper(unit, service, svc);

        System.err.println("!!!!!!!! CREATE DU: " + unit.getId() + " " + (System.currentTimeMillis() - start) +"ms");
        return Result.good();
    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        return Collections.emptyList();
    }

    @Override
    public UnitRef getRef() {
        return ref;
    }

    @Override
    public BasicStateWrapper getWrapper() {
        return unit;
    }

    @Override
    public Result removeBad(InatorContext context, RemoveReason reason) {
        if (reason != RemoveReason.ERROR) {
            return Result.good();
        }
        unit.pause();
        return new Result(UnitState.ERROR, this, String.format("%s: %s", getDisplayName(), unit.getTransitioningMessage()));
    }

    @Override
    public String getDisplayName() {
        return unit == null ? "(missing unit)" : unit.getDisplayName();
    }

    @Override
    public Result pause() {
        return unit == null ? Result.good() : unit.pause();
    }

}
