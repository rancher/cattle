package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.Result;
import io.cattle.platform.inator.Inator.DesiredState;
import io.cattle.platform.inator.UnitRef;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.util.StateUtil;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DeploymentUnitWrapper implements BasicStateWrapper {

    DeploymentUnit unit;
    Service service;
    InatorServices svc;
    Host host;

    public DeploymentUnitWrapper(DeploymentUnit unit, Service service, InatorServices svc) {
        super();
        this.unit = unit;
        this.svc = svc;
        this.service = service;
    }

    public boolean isDeployable() {
        if (host == null && unit.getHostId() != null) {
            host = svc.objectManager.loadResource(Host.class, unit.getHostId());
        }
        if (host == null) {
            return svc.hostDao.hasActiveHosts(unit.getAccountId());
        }
        return StringUtils.isBlank(host.getAgentState()) || CommonStatesConstants.ACTIVE.equals(host.getAgentState());
    }

    public Inator.DesiredState getDesiredState() {
        if (service != null && (CommonStatesConstants.REMOVING.equals(service.getState()) || service.getRemoved() != null)) {
            return DesiredState.REMOVED;
        }
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

    public DeploymentUnit getInternal() {
        return unit;
    }

    public Long getRevisionId() {
        if (unit.getRequestedRevisionId() != null) {
            return unit.getRequestedRevisionId();
        }
        return unit.getRevisionId();
    }

    public Long getRequestRevisionId() {
        return unit.getRequestedRevisionId();
    }

    public Long getAppliedRevisionId() {
        return unit.getRevisionId();
    }

    public String getServiceIndex() {
        return unit.getServiceIndex();
    }

    public UnitRef getRef() {
        return newRef(unit.getHostId(), unit.getServiceIndex(), svc);
    }

    public Long getHostId() {
        Long hostId = unit.getHostId();
        if (hostId != null) {
            return hostId;
        }

        Map<String, Object> labels = DataAccessor.fieldMap(unit, InstanceConstants.FIELD_LABELS);
        Object hostObj = labels.get(ServiceConstants.LABEL_SERVICE_REQUESTED_HOST_ID);
        if (hostObj instanceof Number) {
            return ((Number) hostObj).longValue();
        }

        return null;
    }

    public String getIndex() {
        return unit.getServiceIndex();
    }

    public static UnitRef newRef(Long hostId, String index, InatorServices svc) {
        if (hostId == null) {
            return new UnitRef(ServiceConstants.KIND_DEPLOYMENT_UNIT + "/" + index);
        }
        Object id = svc.idFormatter.formatId(HostConstants.TYPE, hostId);
        return new UnitRef(ServiceConstants.KIND_DEPLOYMENT_UNIT + "/" + id + "/" + index);
    }

    public static String getIndex(UnitRef ref) {
        String refString = ref.toString();
        if (refString.startsWith(ServiceConstants.KIND_DEPLOYMENT_UNIT + "/")) {
            return refString.substring(ServiceConstants.KIND_DEPLOYMENT_UNIT.length() + 1);
        }
        return null;
    }

    public String getUuid() {
        return unit.getUuid();
    }

    public Long getId() {
        return unit.getId();
    }

    public String getDisplayName() {
        return String.format("%s(%s)", unit.getKind(), svc.idFormatter.formatId(unit.getKind(), unit.getId()));
    }

    public Result pause() {
        if (!StateUtil.isPaused(unit.getState())) {
            svc.processManager.scheduleProcessInstance(ServiceConstants.PROCESS_DU_PAUSE, unit, null);
        }
        return Result.good();
    }

    public Long getStackId() {
        return unit.getStackId();
    }

    public Long getServiceId() {
        return unit.getServiceId();
    }

    public void setApplied() {
        boolean changed = false;

        if (unit.getRequestedRevisionId() != null && !unit.getRevisionId().equals(unit.getRequestedRevisionId())) {
            unit.setRevisionId(unit.getRequestedRevisionId());
            changed = true;
        }

        if (service != null) {
            Long serviceRestart = DataAccessor.fieldLong(service, ServiceConstants.FIELD_RESTART_TRIGGER);
            if (serviceRestart == null) {
                serviceRestart = 0L;
            }

            if (!getRestartTrigger().equals(serviceRestart)) {
                DataAccessor.setField(unit, ServiceConstants.FIELD_RESTART_TRIGGER, serviceRestart);
                changed = true;
            }
        }

        if (changed) {
            svc.objectManager.persist(unit);
            svc.triggerServiceReconcile(unit.getServiceId());
        }
    }

    public Long getRestartTrigger() {
        Long val = DataAccessor.fieldLong(unit, ServiceConstants.FIELD_RESTART_TRIGGER);
        return val == null ? 0L : val;
    }

    public String getTransitioningMessage() {
        return TransitioningUtils.getTransitioningError(unit);
    }

}