package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.model.Instance;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Date;

public class InstanceWrapper implements BasicStateWrapper {

    Instance instance;
    LaunchConfig launchConfig;
    Services svc;

    public InstanceWrapper(Instance instance, LaunchConfig launchConfig, Services svc) {
        super();
        this.instance = instance;
        this.launchConfig = launchConfig;
        this.svc = svc;
    }

    public LaunchConfig getLaunchConfig() {
        return launchConfig;
    }

    @Override
    public boolean remove() {
        if (instance.getRemoved() != null) {
            return true;
        }
        svc.processManager.remove(instance, null);
        return false;
    }

    @Override
    public void create() {
        svc.processManager.create(instance, null);
    }

    @Override
    public void activate() {
        svc.processManager.start(instance, null);
    }

    @Override
    public void deactivate() {
        svc.processManager.stop(instance, null);
    }

    @Override
    public String getState() {
        return instance.getState();
    }

    @Override
    public String getHealthState() {
        return instance.getHealthState();
    }

    @Override
    public Date getRemoved() {
        return instance.getRemoved();
    }

    @Override
    public ObjectMetaDataManager getMetadataManager() {
        return svc.metadataManager;
    }

    public Long getId() {
        return instance.getId();
    }

}