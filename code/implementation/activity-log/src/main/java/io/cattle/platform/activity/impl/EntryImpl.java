package io.cattle.platform.activity.impl;

import io.cattle.platform.activity.Entry;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceLog;

public class EntryImpl implements Entry {
    ActivityLogImpl logImpl;
    Service owner;
    ServiceLog auditLog;
    boolean failed;
    String message;

    public EntryImpl(ActivityLogImpl logImpl, Service owner, ServiceLog auditLog) {
        super();
        this.logImpl = logImpl;
        this.owner = owner;
        this.auditLog = auditLog;
    }

    @Override
    public void close() {
        logImpl.close(this);
    }

    @Override
    public void exception(Throwable t) {
        logImpl.exception(this, t);
    }

    public Object getOwner() {
        return owner;
    }

}
