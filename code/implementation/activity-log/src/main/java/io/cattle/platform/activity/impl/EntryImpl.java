package io.cattle.platform.activity.impl;

import io.cattle.platform.activity.Entry;
import io.cattle.platform.core.model.AuditLog;

public class EntryImpl implements Entry {
    ActivityLogImpl logImpl;
    Object owner;
    AuditLog auditLog;
    
    public EntryImpl(ActivityLogImpl logImpl, Object owner, AuditLog auditLog) {
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
