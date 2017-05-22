package io.cattle.platform.inator.planner.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.planner.UnitPlanner;
import io.cattle.platform.inator.unit.DeploymentUnitUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScaleServicePlanner implements UnitPlanner {

    ServiceWrapper service;
    Set<UnitRef> desiredSet;
    InatorServices svc;

    public ScaleServicePlanner(Service service, InatorServices svc) {
        super();
        this.service = new ServiceWrapper(service, svc);
        this.svc = svc;
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        Map<UnitRef, Unit> result = new HashMap<>(context.getUnits());
        FillInLimit limit = new FillInLimit();
        int scale = service.getScale();
        for (int i = result.size() ; i < scale ; i++) {
            DeploymentUnitUnit unit = new DeploymentUnitUnit(service, Integer.toString(i+1), svc);
            if (!limit.add(result, unit)) {
                break;
            }
        }

        return result;
    }

    @Override
    public Set<UnitRef> getDesiredUnits() {
        if (desiredSet != null) {
            return desiredSet;
        }

        Set<UnitRef> desiredSet = new HashSet<>();

        int scale = service.getScale();
        for (int i = 0 ; i < scale ; i++) {
            UnitRef ref = DeploymentUnitWrapper.newRef(null, Integer.toString(i+1), svc);
            desiredSet.add(ref);
        }

        return this.desiredSet = desiredSet;
    }

}
