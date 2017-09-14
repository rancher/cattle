package io.cattle.platform.inator.lock;

import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.inator.wrapper.DeploymentUnitWrapper;
import io.cattle.platform.lock.definition.AbstractBlockingLockDefintion;

public class VolumeDefineLock extends AbstractBlockingLockDefintion {

    public VolumeDefineLock(VolumeTemplate template, DeploymentUnitWrapper unit) {
        super("VOLUME.DEFINE." + template.getId() + "." + (template.getPerContainer() ? unit.getId() : null));
    }

}