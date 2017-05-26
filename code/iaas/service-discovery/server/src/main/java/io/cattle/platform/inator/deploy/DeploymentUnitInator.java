package io.cattle.platform.inator.deploy;

import io.cattle.platform.core.dao.ServiceDao.VolumeData;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.Revision;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.Unit.UnitState;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.unit.InstanceUnit;
import io.cattle.platform.inator.unit.VolumeUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.RevisionWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentUnitInator implements Inator {

    Map<Long, ServiceIndex> indexes = new HashMap<>();
    Map<Long, RevisionWrapper> revisions = new HashMap<>();
    DeploymentUnitWrapper unit;
    StackWrapper stack;
    InatorServices svc;
    Map<UnitRef, Unit> savedDesiredUnits;
    ProcessKickamajig processKicker;

    public DeploymentUnitInator(DeploymentUnitWrapper unit, StackWrapper stack, InatorServices svc) {
        super();
        this.unit = unit;
        this.svc = svc;
        this.stack = stack;
        this.processKicker = new ProcessKickamajig(svc);
    }

    @Override
    public List<Unit> collect() {
        if (!unit.isDeployable()) {
            return Collections.emptyList();
        }

        List<Unit> units = new ArrayList<>();
        units.addAll(collectInstances());
        units.addAll(collectVolumes());

        return units;
    }

    protected List<Unit> collectInstances() {
        return svc.serviceDao.getInstanceData(unit.getId(), unit.getUuid()).stream()
            .map((x) -> (Unit)toInstanceUnit(x.instance, x.serviceIndex, x.serviceExposeMap))
            .collect(Collectors.toList());
    }

    protected List<Unit> collectVolumes() {
        List<Unit> result = new ArrayList<>();
        for (VolumeData volumeData : svc.serviceDao.getVolumeData(unit.getId())) {
            result.add(new VolumeUnit(volumeData.template, volumeData.volume, svc));
        }
        return result;
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        if (!unit.isDeployable()) {
            return Collections.emptyMap();
        }

        Map<UnitRef, Unit> units = new HashMap<>(context.getUnits());

        getDesiredMap().forEach((k, v) -> {
            if (!units.containsKey(k)) {
                units.put(k, v);
            }
        });

        return units;
    }

    protected Map<UnitRef, Unit> getDesiredMap() {
        if (savedDesiredUnits != null) {
            return savedDesiredUnits;
        }

        Map<UnitRef, Unit> units = new HashMap<>();

        getDesiredLaunchConfigs().forEach((name, lc) -> {
            InstanceUnit instanceUnit = new InstanceUnit(name, lc, stack, unit);
            units.put(instanceUnit.getRef(), instanceUnit);
        });

        getDesiredLaunchConfigs().forEach((name, lc) -> {
            lc.getDependencies().forEach((ref, unit) -> {
                if (!units.containsKey(ref)) {
                    units.put(ref, unit);
                }
            });
        });

        return savedDesiredUnits = units;
    }

    protected Map<String, LaunchConfig> getDesiredLaunchConfigs() {
        return getRevision().getLaunchConfigs();
    }

    public RevisionWrapper getRevision() {
        return buildRevisionWrapper(unit.getRevisionId());
    }

    protected RevisionWrapper buildRevisionWrapper(Long id) {
        RevisionWrapper cached = revisions.get(id);
        if (cached != null) {
            return cached;
        }

        Revision rev = svc.objectManager.loadResource(Revision.class, id);
        RevisionWrapper currentRevision = null;
        if (unit.getRequestRevisionId() != null && !unit.getRequestRevisionId().equals(id)) {
            currentRevision = buildRevisionWrapper(unit.getRequestRevisionId());
        }

        RevisionWrapper revisionWrapper = new RevisionWrapper(stack, rev, currentRevision, svc);
        revisions.put(rev.getId(), revisionWrapper);
        return revisionWrapper;
    }

    protected InstanceUnit toInstanceUnit(Instance instance, ServiceIndex index, ServiceExposeMap serviceExposeMap) {
        RevisionWrapper revisionWrapper = revisions.get(instance.getRevisionId());
        if (revisionWrapper == null) {
            revisionWrapper = buildRevisionWrapper(instance.getRevisionId());
        }

        InstanceWrapper instanceWrapper = new InstanceWrapper(instance, serviceExposeMap, index, svc);
        return new InstanceUnit(instanceWrapper, revisionWrapper.getLaunchConfig(instanceWrapper.getLaunchConfigName()));
    }

    @Override
    public Set<UnitRef> getDesiredRefs() {
        return getDesiredMap().keySet();
    }

    @Override
    public DesiredState getDesiredState() {
        return unit.getDesiredState();
    }

    @Override
    public Result postProcess(InatorContext context, Result result) {
        if (result.getState() == UnitState.GOOD) {
            unit.setApplied();
        }
        processKicker.kickProcess(unit.getInternal(), unit.getState(), result.getState());
        return result;
    }

    public DeploymentUnitWrapper getUnit() {
        return unit;
    }

}
