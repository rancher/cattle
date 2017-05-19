package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.model.VolumeTemplate;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Date;
import java.util.Set;

public class VolumeWrapper implements BasicStateWrapper {

    private static Set<String> ADDL_ACTIVE_STATES = CollectionUtils.set(
            CommonStatesConstants.INACTIVE,
            VolumeConstants.STATE_DETACHED);
    private static Set<String> ADDL_INACTIVE_STATES = CollectionUtils.set(
            VolumeConstants.STATE_DETACHED);

    Volume volume;
    VolumeTemplate volumeTemplate;
    InatorServices svc;

    public VolumeWrapper(VolumeTemplate volumeTemplate, Volume volume, InatorServices svc) {
        this.volumeTemplate = volumeTemplate;
        this.volume = volume;
        this.svc = svc;
    }

    @Override
    public void create() {
        svc.processManager.create(volume, null);
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }

    @Override
    public boolean remove() {
        if (!volumeTemplate.getPerContainer()) {
            return true;
        }
        if (volume.getRemoved() != null) {
            return true;
        }
        svc.processManager.remove(volume, null);
        return false;
    }

    @Override
    public String getState() {
        return volume.getState();
    }

    @Override
    public String getHealthState() {
        return volume.getRemoved() == null ? null : HealthcheckConstants.HEALTH_STATE_UNHEALTHY;
    }

    @Override
    public Date getRemoved() {
        return volume.getRemoved();
    }

    @Override
    public ObjectMetaDataManager getMetadataManager() {
        return svc.metadataManager;
    }

    @Override
    public boolean isActive() {
        if (BasicStateWrapper.super.isActive()) {
            return true;
        }
        return ADDL_ACTIVE_STATES.contains(getState());
    }

    @Override
    public boolean isInactive() {
        if (BasicStateWrapper.super.isInactive()) {
            return true;
        }
        return ADDL_INACTIVE_STATES.contains(getState());
    }

    public String getName() {
        return volume.getName();
    }

    public Long getId() {
        return volume.getId();
    }

}
