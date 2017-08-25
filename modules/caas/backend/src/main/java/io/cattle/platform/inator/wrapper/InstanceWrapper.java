package io.cattle.platform.inator.wrapper;

import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.TransitioningUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.DateUtils;
import org.apache.cloudstack.managed.context.NoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class InstanceWrapper implements BasicStateWrapper {

    private static DynamicLongProperty MAX_BACKOFF = ArchaiusUtil.getLong("instance.restart.max.backoff.exponent");

    private static final Logger log = LoggerFactory.getLogger(InstanceWrapper.class);
    private static Set<String> TO_STOP_STATES = CollectionUtils.set(
            InstanceConstants.STATE_STARTING,
            InstanceConstants.STATE_RUNNING);

    Instance instance;
    InatorServices svc;

    public InstanceWrapper(Instance instance, InatorServices svc) {
        super();
        this.instance = instance;
        this.svc = svc;
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
        obj.put(InstanceConstants.FIELD_CREATE_ONLY, true);
        svc.processManager.create(instance, obj);
    }

    @Override
    public void activate() {
        Long restartCount = DataAccessor.fieldLong(instance, InstanceConstants.FIELD_START_RETRY_COUNT);
        Date lastStop = DataAccessor.fieldDate(instance, InstanceConstants.FIELD_STOPPED);
        if (restartCount != null &&
                lastStop != null &&
                restartCount > 0 &&
                InstanceConstants.STATE_STOPPED.equals(instance.getState()) &&
                shouldRestart()) {
            if (restartCount > MAX_BACKOFF.get()) {
                restartCount = MAX_BACKOFF.get();
            }

            long runAfter = lastStop.getTime() + (long)(Math.pow(2, restartCount)*1000);
            if (runAfter > System.currentTimeMillis()) {
                svc.scheduledExecutorService.schedule((NoException)() -> {
                    svc.triggerDeploymentUnitReconcile(instance.getDeploymentUnitId());
                }, runAfter - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                return;
            }
        }
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
        return svc.objectMetadataManager;
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
        return TransitioningUtils.getTransitioningErrorMessage(instance);
    }

    public void setDesired(boolean desired) {
        boolean changed = false;
        if (instance.getDesired().booleanValue() != desired) {
            instance.setDesired(desired);
            changed = true;
        }

        if (desired && instance.getUpgradeTime() != null) {
            instance.setUpgradeTime(null);
            changed = true;
        } else if (!desired && instance.getUpgradeTime() == null) {
            instance.setUpgradeTime(new Date());
            changed = true;
        }

        if (changed) {
            svc.objectManager.persist(instance);
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
            } else // For not service managed, never started, stopped and createOnly=true is considered active
                return InstanceConstants.STATE_STOPPED.equals(instance.getState()) &&
                        DataAccessor.fieldBool(instance, InstanceConstants.FIELD_CREATE_ONLY);
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
        return DataAccessor.fieldString(instance, InstanceConstants.FIELD_LAUNCH_CONFIG_NAME);
    }

    public boolean isServiceManaged() {
        return instance.getServiceId() != null;
    }

    public String getName() {
        return instance.getName();
    }
}