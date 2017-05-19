package io.cattle.platform.activity.impl;

import io.cattle.platform.activity.Entry;
import io.cattle.platform.core.model.ServiceLog;

public class EntryImpl implements Entry {
    ActivityLogImpl logImpl;
    Long serviceId, deploymentUnitId;
    ServiceLog auditLog;
    boolean failed;
    boolean waiting;
    String message;

    public EntryImpl(ActivityLogImpl logImpl, Long serviceId, Long deploymentUnitId, ServiceLog auditLog) {
        super();
        this.logImpl = logImpl;
        this.serviceId = serviceId;
        this.auditLog = auditLog;
        this.deploymentUnitId = deploymentUnitId;
    }

    @Override
    public void close() {
        logImpl.close(this);
    }

    @Override
    public void exception(Throwable t) {
        logImpl.exception(this, t);
    }

    public Long getServiceId() {
        return serviceId;
    }

    public Long getDeploymentUnitId() {
        return deploymentUnitId;
    }

}
