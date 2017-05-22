package io.cattle.platform.inator.unit;

import static io.cattle.platform.core.model.tables.VolumeTable.*;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.InatorContext;
import io.cattle.platform.inator.InstanceBindable;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Unit;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.deploy.DeploymentUnitInator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.lock.VolumeDefineLock;
import io.cattle.platform.inator.wrapper.BasicStateWrapper;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.inator.wrapper.StackWrapper;
import io.cattle.platform.inator.wrapper.VolumeWrapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.resource.UUID;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class VolumeUnit implements Unit, BasicStateUnit, InstanceBindable {

    VolumeTemplate volumeTemplate;
    VolumeWrapper volume;
    InatorServices svc;

    public VolumeUnit(VolumeTemplate volumeTemplate, Volume volume, InatorServices svc) {
        super();
        this.volumeTemplate = volumeTemplate;
        this.svc = svc;
        this.volume = new VolumeWrapper(volumeTemplate, volume, svc);
    }

    public VolumeUnit(VolumeTemplate volumeTemplate, InatorServices svc) {
        super();
        this.volumeTemplate = volumeTemplate;
        this.svc = svc;
    }

    @Override
    public Result define(InatorContext context, boolean desired) {
        if (!desired) {
            return Result.good();
        }

        Inator inator = context.getInator();
        if (!(context.getInator() instanceof DeploymentUnitInator)) {
            return new Result(UnitState.ERROR, this, "Can only create volumes in a deployment unit");
        }

        DeploymentUnitWrapper deploymentUnit = ((DeploymentUnitInator) inator).getUnit();
        VolumeWrapper volume = lookup(deploymentUnit);
        if (volume != null) {
            this.volume = volume;
        }

        if (this.volume == null && volumeTemplate.getExternal()) {
            return new Result(UnitState.ERROR, this, String.format("Failed to find volume %s", volumeTemplate.getName()));
        } else if (this.volume == null) {
            this.volume = svc.lockManager.lock(getDefineLock(deploymentUnit), () -> {
                VolumeWrapper foundVolume = lookup(deploymentUnit);
                return foundVolume != null ? foundVolume : doDefine(deploymentUnit);
            });
        }

        return Result.good();
    }

    protected LockDefinition getDefineLock(DeploymentUnitWrapper unit) {
        return new VolumeDefineLock(volumeTemplate, unit);
    }

    protected VolumeWrapper doDefine(DeploymentUnitWrapper unit) {
        Long deploymentUnitId = null;
        StackWrapper stack = new StackWrapper(svc.objectManager.loadResource(Stack.class, unit.getStackId()));
        String name = String.format("%s_%s_", stack.getName(), volumeTemplate.getName());

        if (volumeTemplate.getPerContainer()) {
            deploymentUnitId = unit.getId();
            name = String.format("%s%s_", name, unit.getServiceIndex());
        }

        name += UUID.randomUUID().toString().substring(0, 5);

        Volume volume = svc.objectManager.create(Volume.class,
                VOLUME.DEPLOYMENT_UNIT_ID, deploymentUnitId,
                ObjectMetaDataManager.NAME_FIELD, name,
                ObjectMetaDataManager.ACCOUNT_FIELD, volumeTemplate.getAccountId(),
                VOLUME.STACK_ID, stack.getId(),
                VOLUME.VOLUME_TEMPLATE_ID, volumeTemplate.getId(),
                VolumeConstants.FIELD_VOLUME_DRIVER_OPTS, DataAccessor.fieldMap(volumeTemplate, VolumeConstants.FIELD_VOLUME_DRIVER_OPTS),
                VolumeConstants.FIELD_VOLUME_DRIVER, volumeTemplate.getDriver());
        return new VolumeWrapper(volumeTemplate, volume, svc);
    }

    protected VolumeWrapper lookup(DeploymentUnitWrapper unit) {
        Volume volume = null;
        if (volumeTemplate.getExternal()) {
            volume = svc.objectManager.findAny(Volume.class,
                VOLUME.ACCOUNT_ID, volumeTemplate.getAccountId(),
                VOLUME.NAME, volumeTemplate.getName(),
                VOLUME.REMOVED, null);
        } else if (volumeTemplate.getPerContainer()) {
            volume = svc.objectManager.findAny(Volume.class,
                VOLUME.REMOVED, null,
                VOLUME.DEPLOYMENT_UNIT_ID, unit.getId(),
                VOLUME.VOLUME_TEMPLATE_ID, volumeTemplate.getId());
        } else {
            volume = svc.objectManager.findAny(Volume.class,
                VOLUME.REMOVED, null,
                VOLUME.VOLUME_TEMPLATE_ID, volumeTemplate.getId());
        }
        return volume == null ? null : new VolumeWrapper(volumeTemplate, volume, svc);
    }

    @Override
    public BasicStateWrapper getWrapper() {
        return volume;
    }

    @Override
    public Result removeBad(InatorContext context, RemoveReason reason) {
        return volume.remove() ? Result.good() : new Result(UnitState.WAITING, this, String.format("Removing volume for %s", getDisplayName()));
    }

    @Override
    public Collection<UnitRef> dependencies(InatorContext context) {
        return Collections.emptyList();
    }

    @Override
    public UnitRef getRef() {
        return new UnitRef("volume/" + volumeTemplate.getName());
    }

    @Override
    public String getDisplayName() {
        return String.format("volumeTemplate(%s)", volumeTemplate.getName());
    }

    @Override
    public void bind(InatorContext context, Map<String, Object> instanceData) {
        if (volume == null) {
            return;
        }

        String prefix = volumeTemplate.getName() + ":";

        @SuppressWarnings("unchecked")
        List<String> volumes = (List<String>) CollectionUtils.toList(instanceData.get(InstanceConstants.FIELD_DATA_VOLUMES));
        Map<String, Object> dataVolumes = CollectionUtils.toMap(instanceData.get(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS));
        boolean changed = false;
        List<String> newVolumes = new ArrayList<>();

        for (int i = 0 ; i < volumes.size() ; i++ ) {
            String volume = volumes.get(i);
            if (volume.startsWith(prefix)) {
                String path = volume.substring(prefix.length());
                dataVolumes.put(path, this.volume.getId());
                changed = true;
            } else {
                newVolumes.add(volume);
            }
        }

        if (changed) {
            instanceData.put(InstanceConstants.FIELD_DATA_VOLUMES, newVolumes);
            instanceData.put(InstanceConstants.FIELD_DATA_VOLUME_MOUNTS, dataVolumes);
        }
    }

}
