package io.cattle.platform.inator.deploy;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.unit.DeploymentUnitUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.ServiceRevisionWrapper;
import io.cattle.platform.inator.wrapper.ServiceWrapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ServiceInator implements Inator {

    ServiceWrapper service;
    ServiceRevisionWrapper serviceRevision;
    Set<UnitRef> desiredSet;
    Services svc;

    public ServiceInator(Service service, Services svc) {
        super();
        this.service = new ServiceWrapper(service);
        this.svc = svc;
        init();
    }

    private void init() {
        ServiceRevision serviceRevision = svc.objectManager.loadResource(ServiceRevision.class,
                service.getService().getRevisionId());
        this.serviceRevision = new ServiceRevisionWrapper(serviceRevision, svc);
    }

    @Override
    public List<Unit> collect() {
        return svc.serviceDao.getDeploymentUnits(service.getService()).values().stream()
            .map((deploymentUnit) -> newUnit(deploymentUnit, 0))
            .collect(Collectors.toList());
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        Map<UnitRef, Unit> result = new HashMap<>(context.getUnits());
        int scale = service.getScale();
        for (int i = result.size() ; i < scale ; i++) {
            DeploymentUnitUnit unit = newUnit(null, i);
            result.put(unit.getRef(), unit);
        }

        return result;
    }

    protected DeploymentUnitUnit newUnit(DeploymentUnit unit, int index) {
        if (unit == null) {
            UnitRef ref = new UnitRef(ServiceConstants.KIND_DEPLOYMENT_UNIT + "/" + (index + 1));
            return new DeploymentUnitUnit(ref, service, false, svc);
        } else {
            return new DeploymentUnitUnit(unit, service, false, svc);
        }
    }

    @Override
    public Set<UnitRef> getDesiredUnits() {
        if (desiredSet != null) {
            return desiredSet;
        }

        Set<UnitRef> desiredSet = new HashSet<>();

        int scale = service.getScale();
        for (int i = 0 ; i < scale ; i++) {
            UnitRef ref = DeploymentUnitWrapper.newRef(false, i+1);
            desiredSet.add(ref);
        }

        return this.desiredSet = desiredSet;
    }

    @Override
    public DesiredState getDesiredState() {
        return service.getDesiredState();
    }

}