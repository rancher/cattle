package io.cattle.platform.activity.impl;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.activity.Entry;
import io.cattle.platform.core.model.AuditLog;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.exception.InstanceException;

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
    public Entry start(Object actor, String type, String message) {
        AuditLog auditLog = newAuditLog(actor);
        auditLog.setEventType(type);
        auditLog.setDescription(message);
        auditLog.setTransactionId(UUID.randomUUID().toString());
        if (entries.size() > 0) {
            auditLog.setSubLog(true);
            auditLog.setEventType(entries.peek().auditLog.getEventType() + "." + type);
        }
        EntryImpl impl = new EntryImpl(this, actor, objectManager.create(auditLog));
        entries.push(impl);
        return impl;
    }

    
    protected AuditLog newAuditLog(Object obj) {
        Object id = ObjectUtils.getId(obj);
        Object accountId = ObjectUtils.getAccountId(obj);
        AuditLog auditLog = objectManager.newRecord(AuditLog.class);
        auditLog.setLevel("info");
        auditLog.setCreated(new Date());
        auditLog.setResourceType(ObjectUtils.getKind(obj));
        if (id instanceof Long) {
            auditLog.setResourceId((Long)id);
        }
        if (accountId instanceof Long) {
            auditLog.setAccountId((Long)accountId);
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
        log.setEndTime(new Date());
        objectManager.create(log);
    }

    @Override
    public void instance(Instance instance, String operation, String reason) {
        if (instance == null) {
            return;
        }
        AuditLog log = newSubEntry(entries.peek(), "");
        log.setEventType("service.instance." + operation);
        log.setEndTime(new Date());
        log.setDescription(reason);
        log.setInstanceId(instance.getId());
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
        log.setInstanceId(getInstanceIdFromThrowable(t));
        log.setDescription(t.getMessage());
        log.setLevel("info");
        objectManager.create(log);
    }
    
    protected Long getInstanceIdFromThrowable(Throwable t) {
        if (t instanceof InstanceException) {
            Object obj = ((InstanceException) t).getInstance();
            if (obj instanceof Instance) {
                return ((Instance) obj).getId();
            }
        }
        return null;
    }

}
