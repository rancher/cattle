package io.cattle.platform.inator.unit;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.Collection;
import java.util.Collections;

public class DeploymentUnitUnit implements Unit, BasicStateUnit {

    DeploymentUnitWrapper unit;
    ServiceWrapper service;
    UnitRef ref;
    boolean global;
    Services svc;

    public DeploymentUnitUnit(DeploymentUnit unit, ServiceWrapper service, boolean global, Services svc) {
        this.service = service;
        this.unit = new DeploymentUnitWrapper(unit, svc);
        this.global = global;
        this.ref = this.unit.getRef(global);
        this.svc = svc;
    }

    public DeploymentUnitUnit(UnitRef ref, ServiceWrapper service, boolean global, Services svc) {
        this.service = service;
        this.ref = ref;
        this.global = global;
        this.service = service;
        this.svc = svc;
    }

    @Override
    public void define(InatorContext context) {
        if (unit != null) {
            return;
        }

        Service service = this.service.getService();
        DeploymentUnit unit = svc.serviceDao.createDeploymentUnit(service.getAccountId(), service.getId(),
                service.getStackId(),
                null, DeploymentUnitWrapper.getIndex(ref), service.getRevisionId());
        this.unit = new DeploymentUnitWrapper(unit, svc);
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
    public UnitState removeBad(InatorContext context) {
        // TODO TRIGGER BAD CLEANUP
        return null;
    }
}
