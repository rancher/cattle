package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.Services;
import io.cattle.platform.inator.util.StateUtil;
import io.cattle.platform.object.meta.ObjectMetaDataManager;

import java.util.Date;

public class DeploymentUnitWrapper implements BasicStateWrapper {

    DeploymentUnit unit;
    Services svc;

    public DeploymentUnitWrapper(DeploymentUnit unit, Services svc) {
        super();
        this.unit = unit;
        this.svc = svc;
    }

    public boolean isCleanup() {
        return Boolean.TRUE.equals(unit.getCleanup());
    }

    public Inator.DesiredState getDesiredState() {
        return StateUtil.getDesiredState(unit.getState(), unit.getRemoved());
    }

    @Override
    public boolean remove() {
        if (unit.getRemoved() != null) {
            return true;
        }
        svc.processManager.remove(unit, null);
        return false;
    }

    @Override
    public boolean isTransitioning() {
        return svc.metadataManager.isTransitioningState(DeploymentUnit.class, unit.getState());
    }

    @Override
    public void create() {
        svc.processManager.createThenActivate(unit, null);
    }

    @Override
    public void activate() {
        svc.processManager.activate(unit, null);
    }

    @Override
    public void deactivate() {
        svc.processManager.deactivate(unit, null);
    }

    @Override
    public String getState() {
        return unit.getState();
    }

    @Override
    public String getHealthState() {
        return unit.getHealthState();
    }

    @Override
    public Date getRemoved() {
        return unit.getRemoved();
    }

    @Override
    public ObjectMetaDataManager getMetadataManager() {
        return svc.metadataManager;
    }

    public DeploymentUnit getUnit() {
        return unit;
    }

    public Long getRevisionId() {
        return unit.getRevisionId();
    }

    public int getServiceIndex() {
        String str = unit.getServiceIndex();
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    public UnitRef getRef(boolean global) {
        return newRef(global, unit);
    }

    public static UnitRef newRef(boolean global, DeploymentUnit unit) {
        if (global) {
            throw new IllegalStateException("TODO!!!!");
        }

        return new UnitRef(ServiceConstants.KIND_DEPLOYMENT_UNIT + "/" + unit.getServiceIndex());
    }

    public static UnitRef newRef(boolean global, int index) {
        if (global) {
            throw new IllegalStateException("TODO!!!!");
        }

        return new UnitRef(ServiceConstants.KIND_DEPLOYMENT_UNIT + "/" + index);
    }

    public static String getIndex(UnitRef ref) {
        return ref.toString().substring(ServiceConstants.KIND_DEPLOYMENT_UNIT.length() + 1);
    }

    public String getUuid() {
        return unit.getUuid();
    }

    public Long getId() {
        return unit.getId();
    }

}