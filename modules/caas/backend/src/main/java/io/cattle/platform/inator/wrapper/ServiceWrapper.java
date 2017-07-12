package io.cattle.platform.inator.wrapper;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.GenericObject;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.inator.Inator;
import io.cattle.platform.inator.factory.InatorServices;
import io.cattle.platform.inator.util.StateUtil;
import io.cattle.platform.object.util.DataAccessor;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServiceWrapper {

    Service service;
    InatorServices svc;

    public ServiceWrapper(Service service, InatorServices svc) {
        this.service = service;
        this.svc = svc;
    }

    public boolean isActive() {
        return StateUtil.isActive(getState());
    }

    public boolean isHealthy() {
        return StateUtil.isHealthy(service.getHealthState());
    }

    public Inator.DesiredState getDesiredState() {
        return StateUtil.getDesiredState(service.getState(), service.getRemoved());
    }

    public Service getService() {
        return service;
    }

    public int getScale() {
        Integer scale = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE);
        if (scale == null) {
            scale = 0;
        }

        Integer min = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE_MIN);
        if (min != null && scale < min) {
            scale = min;
        }

        Integer max = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE_MAX);
        if (max != null && scale > max) {
            scale = max;
        }

        Integer incr = DataAccessor.fieldInteger(service, ServiceConstants.FIELD_SCALE_INCREMENT);
        if (incr != null && ((scale-min)%incr) != 0) {
            scale = scale - ((scale-min)%incr);
        }

        return scale;
    }

    public Long getRevisionId() {
        return service.getRevisionId();
    }

    public Long getBatchSize() {
        Long batchSize = DataAccessor.fieldLong(service, ServiceConstants.FIELD_BATCHSIZE);
        if (batchSize == null) {
            batchSize = 1L;
        }
        return batchSize < 1L ? 1L : batchSize;
    }

    public Long getInterval() {
        Long interval = DataAccessor.fieldLong(service, ServiceConstants.FIELD_INTERVAL_MILLISEC);
        if (interval == null) {
            interval = 1000L;
        }
        return interval < 1000L ? 1000L : interval;
    }

    public Long getLastRun() {
        Long lastRun = DataAccessor.fieldLong(service, ServiceConstants.FIELD_UPGRADE_LAST_RUN);
        if (lastRun == null) {
            lastRun = 0L;
        }
        return lastRun;
    }

    public String getState() {
        return service.getState();
    }

    public void activate() {
        svc.processManager.activate(service, null);
    }

    public Object getId() {
        return service.getId();
    }

    public Service getInternal() {
        return service;
    }

    public Long getAccountId() {
        return service.getAccountId();
    }

    public void setUpgradeLastRunToNow() {
        svc.objectManager.setFields(service, ServiceConstants.FIELD_UPGRADE_LAST_RUN, System.currentTimeMillis());
    }

    public Map<String, GenericObject> getPullTasks() {
        Map<String, GenericObject> pullTasks = new HashMap<>();

        String value = svc.dataDao.get(pullKey());
        if (StringUtils.isBlank(value)) {
            return pullTasks;
        }

        Map<String, Object> ids;
        try {
            ids = svc.jsonMapper.readValue(value);
        } catch (IOException e) {
            return pullTasks;
        }

        for (Map.Entry<String, Object> entry : ids.entrySet()) {
            Object id = entry.getValue();
            if (id == null) {
                continue;
            }

            GenericObject task = svc.objectManager.loadResource(GenericObject.class, id.toString());
            if (task != null) {
                pullTasks.put(entry.getKey(), task);
            }
        }

        return pullTasks;
    }

    protected String pullKey() {
        return "service.pull.task." + service.getId();
    }

    public void savePullTasks(Map<String, GenericObject> newPullTasks) {
        Map<String, Long> ids = new HashMap<>();
        newPullTasks.forEach((k, v) -> ids.put(k, v.getId()));
        try {
            String value = svc.jsonMapper.writeValueAsString(ids);
            svc.dataDao.save(pullKey(), true, value);
        } catch (IOException e) {
            return;
        }
    }

    public String getName() {
        return service.getName();
    }

    public Long getStackId() {
        return service.getStackId();
    }

    public Long getPreviousRevisionId() {
        return service.getPreviousRevisionId();
    }

    public Long getRestartTrigger() {
        Long val = DataAccessor.fieldLong(service, ServiceConstants.FIELD_RESTART_TRIGGER);
        return val == null ? 0L : val;
    }

}
