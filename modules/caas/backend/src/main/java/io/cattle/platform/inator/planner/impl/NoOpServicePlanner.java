package io.cattle.platform.inator.planner.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.planner.UnitPlanner;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NoOpServicePlanner implements UnitPlanner {

    ServiceWrapper service;
    Set<UnitRef> desiredSet = new HashSet<>();
    InatorServices svc;

    public NoOpServicePlanner(Service service, InatorServices svc) {
        super();
        this.service = new ServiceWrapper(service, svc);
        this.svc = svc;
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        return new HashMap<UnitRef, Unit>();
    }

    @Override
    public Set<UnitRef> getDesiredUnits() {
        return new HashSet<UnitRef>();
    }

}
