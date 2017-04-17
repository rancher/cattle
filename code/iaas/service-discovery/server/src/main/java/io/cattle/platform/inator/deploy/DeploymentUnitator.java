package io.cattle.platform.inator.deploy;

import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.core.model.ServiceRevision;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.inator.unit.InstanceUnit;
import io.cattle.platform.inator.unit.VolumeUnit;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.InstanceWrapper;
import io.cattle.platform.inator.wrapper.ServiceRevisionWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.cattle.platform.inator.wrapper.VolumeTemplateWrapper;
import io.cattle.platform.inator.wrapper.VolumeWrapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DeploymentUnitator implements Inator {

    Map<Long, ServiceIndex> indexes = new HashMap<>();
    Map<Long, ServiceRevisionWrapper> revisions = new HashMap<>();
    DeploymentUnitWrapper unit;
    StackWrapper stack;
    Services svc;

    public DeploymentUnitator(DeploymentUnit unit, StackWrapper stack, Services svc) {
        super();
        this.unit = new DeploymentUnitWrapper(unit, svc);
        this.svc = svc;
        this.stack = stack;
    }

    @Override
    public List<Unit> collect() {
        List<Unit> units = collectInstances().stream()
            .map((instance) -> new InstanceUnit(instance, unit.isCleanup()))
            .collect(Collectors.toList());
        List<Unit> volumeUnits = collectVolumes().stream()
            .map((volume) -> new VolumeUnit(volume))
            .collect(Collectors.toList());

        units.addAll(volumeUnits);
        return units;
    }

    public List<InstanceWrapper> collectInstances() {
        return svc.serviceDao.getInstanceAndIndex(unit.getId(), unit.getUuid()).entrySet().stream()
            .map((entry) -> toWrapper(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    @Override
    public Map<UnitRef, Unit> fillIn(InatorContext context) {
        Map<UnitRef, Unit> units = new HashMap<>(context.getUnits());

        getDesiredSet().forEach((k, v) -> {
            if (!units.containsKey(k)) {
                units.put(k, v);
            }
        });

        return units;
    }

    protected Map<UnitRef, Unit> getDesiredSet() {
        Map<UnitRef, Unit> units = new HashMap<>();

        getDesiredLaunchConfigs().forEach((name, lc) -> {
            InstanceUnit instanceUnit = new InstanceUnit(name, lc, unit.isCleanup(), stack, unit);
            units.put(instanceUnit.getRef(), instanceUnit);
        });

        getVolumeTemplates().forEach((name, vt) -> {
            VolumeUnit volumeUnit = new VolumeUnit(name, vt);
            units.put(volumeUnit.getRef(), volumeUnit);
        });

        getDesiredLaunchConfigs().forEach((name, lc) -> {
            lc.getDependenciesHolders().forEach((ref, unit) -> {
                if (!units.containsKey(ref)) {
                    units.put(ref, unit);
                }
            });
        });

        return units;
    }

    protected Map<String, LaunchConfig> getDesiredLaunchConfigs() {
        return getServiceRevision().getLaunchConfigs();
    }

    protected ServiceRevisionWrapper getServiceRevision() {
        ServiceRevisionWrapper wrapper = revisions.get(unit.getRevisionId());
        return wrapper == null ? buildRevisionWrapper(unit.getRevisionId()) : wrapper;
    }

    protected ServiceRevisionWrapper buildRevisionWrapper(Long id) {
        ServiceRevision rev = svc.objectManager.loadResource(ServiceRevision.class, id);
        ServiceRevisionWrapper serviceRevisionWrapper = new ServiceRevisionWrapper(rev, svc);
        revisions.put(rev.getId(), serviceRevisionWrapper);
        return serviceRevisionWrapper;
    }

    protected InstanceWrapper toWrapper(Instance instance, ServiceIndex index) {
        ServiceRevisionWrapper serviceRevisionWrapper = revisions.get(instance.getServiceRevisionId());
        if (serviceRevisionWrapper == null) {
            serviceRevisionWrapper = buildRevisionWrapper(instance.getServiceRevisionId());
        }

        return new InstanceWrapper(instance,
                serviceRevisionWrapper.getLaunchConfigs().get(index.getLaunchConfigName()), svc);
    }

    public List<VolumeWrapper> collectVolumes() {
        // TODO:
        return Collections.emptyList();
    }

    public Map<String, VolumeTemplateWrapper> getVolumeTemplates() {
        // TODO:
        return Collections.emptyMap();
    }

    @Override
    public Set<UnitRef> getDesiredUnits() {
        return getDesiredSet().keySet();
    }

    @Override
    public DesiredState getDesiredState() {
        return unit.getDesiredState();
    }

}
