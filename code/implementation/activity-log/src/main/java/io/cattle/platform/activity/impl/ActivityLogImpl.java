package io.cattle.platform.activity.impl;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.activity.Entry;
import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;

import java.util.Date;
import java.util.Stack;
import java.util.UUID;

public class ActivityLogImpl implements ActivityLog {

    ObjectManager objectManager;
    Stack<EntryImpl> entries = new Stack<>();

    public ActivityLogImpl(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

    @Override
    public Entry start(Object actor, String type) {
        AuditLog auditLog = newAuditLog(actor);
        auditLog.setEventType(type);
        auditLog.setTransactionId(UUID.randomUUID().toString());
        if (entries.size() > 0) {
            auditLog.setSubLog(true);
        }
        EntryImpl impl = new EntryImpl(this, actor, objectManager.create(auditLog));
        entries.push(impl);
        return impl;
    }

    
    protected AuditLog newAuditLog(Object obj) {
        Object id = ObjectUtils.getId(obj);
        AuditLog auditLog = objectManager.newRecord(AuditLog.class);
        auditLog.setCreated(new Date());
        auditLog.setResourceType(ObjectUtils.getKind(obj));
        if (id instanceof Long) {
            auditLog.setResourceId((Long)id);
        }
        if (obj instanceof Service) {
            auditLog.setServiceId(((Service) obj).getId());
        }
        return auditLog;
    }
    
    protected void close(EntryImpl entry) {
        entry.auditLog.setEndTime(new Date());
        entries.pop();
        objectManager.persist(entry.auditLog);
    }

    @Override
    public void info(String message, Object... args) {
        String desc = String.format(message, args);
        AuditLog log = newSubEntry(entries.peek(), "info");
        log.setDescription(desc);
        objectManager.create(log);
    }
    
    protected AuditLog newSubEntry(EntryImpl entryImpl, String suffix) {
        AuditLog log = newAuditLog(entryImpl.owner);
        log.setTransactionId(entryImpl.auditLog.getTransactionId());
        log.setEventType(entryImpl.auditLog.getEventType() + "." + suffix);
        log.setSubLog(true);
        log.setEndTime(new Date());
        return log;
    }

    protected void exception(EntryImpl entryImpl, Throwable t) {
        AuditLog log = newSubEntry(entryImpl, "exception");
        log.setDescription(t.getMessage());
        objectManager.create(log);
    }

}
