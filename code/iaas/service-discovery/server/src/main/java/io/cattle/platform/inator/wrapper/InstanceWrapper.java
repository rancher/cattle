package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.core.model.ServiceIndex;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.launchconfig.LaunchConfig;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.DateUtils;

import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceWrapper implements BasicStateWrapper {

    private static final Logger log = LoggerFactory.getLogger(InstanceWrapper.class);
    private static Set<String> TO_STOP_STATES = CollectionUtils.set(
            InstanceConstants.STATE_STARTING,
            InstanceConstants.STATE_RUNNING);

    Instance instance;
    ServiceExposeMap serviceExposeMap;
    ServiceIndex index;
    LaunchConfig launchConfig;
    InatorServices svc;

    public InstanceWrapper(Instance instance, ServiceExposeMap serviceExposeMap, ServiceIndex index, InatorServices svc) {
        super();
        this.instance = instance;
        this.svc = svc;
        this.serviceExposeMap = serviceExposeMap;
        this.index = index;
    }

    @Override
    public boolean remove() {
        if (instance.getRemoved() != null) {
            return true;
        }
        if (TO_STOP_STATES.contains(instance.getState())) {
            svc.processManager.stopThenRemove(instance, null);
            return false;
        }
        if (isTransitioning()) {
            return false;
        }
        svc.processManager.remove(instance, null);
        return false;
    }

    @Override
    public void create() {
        Map<String, Object> obj = new HashMap<>();
        obj.put(InstanceConstants.FIELD_START_ON_CREATE, false);
        svc.processManager.create(instance, obj);
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

    public Instance getInternal() {
        return instance;
    }

    public Date getStartTime() {
        Object obj = CollectionUtils.getNestedValue(instance.getData(), "dockerInspect", "State", "StartedAt");
        if (obj == null) {
            obj = DataAccessor.fieldDate(instance, InstanceConstants.FIELD_LAST_START);
        }

        if (obj instanceof String) {
            try {
                return DateUtils.parse((String) obj);
            } catch (DateTimeParseException e) {
                log.error("Failed to parse date [{}]", obj);
                return null;
            }
        } else if (obj instanceof Date) {
            return (Date) obj;
        }

        return null;
    }

    public String getDisplayName() {
        return String.format("%s(%s)", instance.getKind(),
                svc.idFormatter.formatId(instance.getKind(), instance.getId()));
    }

    public String getErrorMessage() {
        return TransitioningUtils.getTransitioningError(instance);
    }

    public void setDesired(boolean desired) {
        if (instance.getDesired().booleanValue() != desired) {
            instance.setDesired(desired);
            svc.objectManager.persist(instance);
        }

        if (serviceExposeMap == null) {
            return;
        }

        boolean changed = false;
        // If desired, then upgrade should be false
        if (serviceExposeMap.getUpgrade().booleanValue() == desired) {
            serviceExposeMap.setUpgrade(!desired);
            changed = true;
        }

        if (desired && serviceExposeMap.getUpgradeTime() != null) {
            serviceExposeMap.setUpgradeTime(null);
            changed = true;
        } else if (!desired && serviceExposeMap.getUpgradeTime() == null) {
            serviceExposeMap.setUpgradeTime(new Date());
            changed = true;
        }

        if (changed) {
            svc.objectManager.persist(serviceExposeMap);
        }
    }

    @Override
    public boolean isActive() {
        if (BasicStateWrapper.super.isActive()) {
            return true;
        }

        // Only consider restart policy after it's started once
        if (instance.getFirstRunning() == null) {
            if (isServiceManaged()) {
                return false;
            } else if (InstanceConstants.STATE_STOPPED.equals(instance.getState()) &&
                    !DataAccessor.fieldBool(instance, InstanceConstants.FIELD_START_ON_CREATE)) {
                // For not service managed, never started, stopped and startOnCreate=False is considered active
                return true;
            } else {
                return false;
            }
        }

        return !shouldRestart();
    }

    protected boolean shouldRestart() {
        Boolean restart = DataAccessor.fieldBoolean(instance, InstanceConstants.FIELD_SHOULD_RESTART);
        return restart == null ? true : restart;
    }

    public List<String> getPorts() {
        return DataAccessor.fieldStringList(instance, InstanceConstants.FIELD_PORTS);
    }

    public String getRevision() {
        return instance.getVersion();
    }

    public String getLaunchConfigName() {
        if (index != null) {
            return index.getLaunchConfigName();
        }
        return DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME);
    }

    public boolean isServiceManaged() {
        return instance.getServiceId() != null;
    }

}