package io.cattle.platform.inator.planner.impl;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.util.ServiceUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GlobalServicePlanner implements UnitPlanner {

    ServiceWrapper service;
    Set<UnitRef> desiredSet = new HashSet<>();
    InatorServices svc;

    public GlobalServicePlanner(Service service, InatorServices svc) {
        super();
        this.service = new ServiceWrapper(service, svc);
        this.svc = svc;
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        if (desiredSet.size() > 0) {
            throw new IllegalStateException("fillIn should be called only once");

        }
        Map<UnitRef, Unit> result = new HashMap<>(context.getUnits());
        FillInLimit limit = new FillInLimit();

        int currentIndex = 1;
        Set<String> indexes = new HashSet<>();
        Set<Long> hostIds = getHostIds();
        int scale = service.getScale();
        if (scale <= 0) {
            scale = 1;
        }

        Map<Long, Integer> countsNeeded = new HashMap<>();
        for (Long hostId : hostIds) {
            countsNeeded.put(hostId, scale);
        }

        for (Unit existingUnit : context.getUnits().values()) {
            if (existingUnit instanceof DeploymentUnitUnit) {
                DeploymentUnitWrapper unit = ((DeploymentUnitUnit) existingUnit).getDeploymentUnit();
                if (unit == null) {
                    continue;
                }
                Long hostId = unit.getHostId();
                String index = unit.getIndex();
                if (hostId == null || index == null) {
                    continue;
                }

                indexes.add(index);
                Integer count = countsNeeded.get(hostId);
                if (count != null) {
                    int newCount = count - 1;
                    if (newCount >= 0) {
                        desiredSet.add(DeploymentUnitWrapper.newRef(hostId, index, svc));
                    }
                    countsNeeded.put(hostId, newCount);
                }
            }
        }

        outer:
        for (Map.Entry<Long, Integer> entry : countsNeeded.entrySet()) {
            Long hostId = entry.getKey();
            int count = entry.getValue();

            for (int i = 0 ; i < count ; i++) {
                while (indexes.contains(Integer.toString(currentIndex))) {
                    currentIndex++;
                }
                indexes.add(Integer.toString(currentIndex));

                DeploymentUnitUnit newUnit = new DeploymentUnitUnit(service, hostId, Integer.toString(currentIndex), svc);
                if (!limit.add(result, newUnit)) {
                    break outer;
                }
                desiredSet.add(newUnit.getRef());
            }
        }

        return result;
    }

    @Override
    public Set<UnitRef> getDesiredUnits() {
        return desiredSet;
    }

    protected Set<Long> getHostIds() {
        Map<String, String> labels = ServiceUtil.getMergedServiceLabels(service.getInternal());
        List<Long> hostIds = svc.allocationHelper.getAllHostsSatisfyingHostAffinity(service.getClusterId(), labels);
        return new HashSet<>(hostIds);
    }

}
