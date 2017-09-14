package io.cattle.platform.activity.impl;

import io.cattle.platform.activity.ActivityLog;
import io.cattle.platform.activity.Entry;
import io.cattle.platform.async.utils.ResourceTimeoutException;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.DeploymentUnit;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceLog;
import io.cattle.platform.engine.idempotent.IdempotentRetryException;
import io.cattle.platform.engine.process.ExitReason;
import io.cattle.platform.engine.process.ProcessInstanceException;
import io.cattle.platform.engine.process.impl.ProcessExecutionExitException;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.lock.exception.FailedToAcquireLockException;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.object.util.TransitioningUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.exception.DataChangedException;

import java.util.Date;
import java.util.Stack;

public class ActivityLogImpl implements ActivityLog {
    EventService eventService;
    ObjectManager objectManager;
    Stack<EntryImpl> entries = new Stack<>();
    Long accountId;

    public ActivityLogImpl(ObjectManager objectManager, EventService eventService, Long accountId) {
        super();
        this.objectManager = objectManager;
        this.eventService = eventService;
        this.accountId = accountId;
    }

    @Override
    public Entry start(Long serviceId, Long deploymentUnitId, String type, String message) {
        ServiceLog auditLog = newServiceLog(serviceId, deploymentUnitId);
        auditLog.setEventType(type);
        auditLog.setDescription(message);
        auditLog.setTransactionId(io.cattle.platform.util.resource.UUID.randomUUID().toString());
        if (entries.size() > 0) {
            ServiceLog parentLog = entries.peek().auditLog;
            auditLog.setSubLog(true);
            auditLog.setEventType(parentLog.getEventType() + "." + type);
            auditLog.setTransactionId(parentLog.getTransactionId());
        }
        EntryImpl impl = new EntryImpl(this, serviceId, deploymentUnitId, auditLog);
        entries.push(impl);
        return impl;
    }


    protected ServiceLog newServiceLog(Long serviceId, Long deploymentUnitId) {
        ServiceLog auditLog = objectManager.newRecord(ServiceLog.class);
        auditLog.setLevel("info");
        auditLog.setCreated(new Date());
        auditLog.setAccountId(accountId);
        auditLog.setServiceId(serviceId);
        auditLog.setDeploymentUnitId(deploymentUnitId);

        return auditLog;
    }

    protected void close(EntryImpl entry) {
        entries.pop();
        if (entries.size() == 0) {
            String transitioning = null;
            String message = null;
            if (entry.failed) {
                transitioning = ObjectMetaDataManager.TRANSITIONING_ERROR_OVERRIDE;
                message = entry.message;
            } else if (entry.message != null) {
                message = entry.message;
            }
            if (entry.serviceId != null && (entry.failed || !entry.waiting)) {
                for (int i = 0 ; i < 3 ; i++) {
                    try {
                        objectManager.setFields(objectManager.loadResource(Service.class, entry.serviceId),
                                ObjectMetaDataManager.TRANSITIONING_FIELD, transitioning,
                                ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, message);
                        break;
                    } catch (DataChangedException e) {
                    }
                }
                ObjectUtils.publishChanged(eventService, accountId, null, entry.serviceId, ServiceConstants.KIND_SERVICE);
            }
        }
    }

    @Override
    public void info(String message, Object... args) {
        String desc = String.format(message, args);
        ServiceLog log = newSubEntry(entries.peek(), "info");
        log.setDescription(desc);
        log.setEndTime(new Date());
        create(log);
    }

    @Override
    public void error(String message, Object... args) {
        String desc = String.format(message, args);
        ServiceLog log = newSubEntry(entries.peek(), "error");
        log.setDescription(desc);
        log.setEndTime(new Date());
        create(log);

        EntryImpl entry = entries.peek();
        if (!entry.failed) {
            entry.failed = true;
            entry.message = desc;
        }
    }

    @Override
    public void instance(Long instanceId, String operation, String reason, String level) {
        if (instanceId == null) {
            return;
        }
        ServiceLog log = newSubEntry(entries.peek(), "");
        log.setEventType("service.instance." + operation);
        log.setEndTime(new Date());
        log.setDescription(reason);
        log.setInstanceId(instanceId);
        log.setLevel(level);
        create(log);

        if ("error".equals(level)) {
            EntryImpl entry = entries.peek();
            if (!entry.failed) {
                entry.failed = true;
                entry.message = reason;
            }
        }
    }

    protected ServiceLog newSubEntry(EntryImpl entryImpl, String suffix) {
        ServiceLog log = newServiceLog(entryImpl.serviceId, entryImpl.deploymentUnitId);
        log.setTransactionId(entryImpl.auditLog.getTransactionId());
        log.setEventType(entryImpl.auditLog.getEventType() + "." + suffix);
        log.setSubLog(true);
        log.setEndTime(new Date());
        return log;
    }

    protected void exception(EntryImpl entryImpl, Throwable t) {
        if (t instanceof IdempotentRetryException) {
            return;
        }

        if (t instanceof ProcessInstanceException) {
            return;
        }

        entryImpl.failed = true;
        entryImpl.message = t.getMessage();
        ServiceLog log = newSubEntry(entryImpl, "exception");
        log.setInstanceId(getInstanceIdFromThrowable(t));
        log.setDeploymentUnitId(getDeploymentIdFromThrowable(t));
        log.setDescription(t.getMessage());
        log.setLevel("error");

        if (t instanceof DataChangedException) {
            entryImpl.failed = false;
            log.setLevel("info");
            log.setDescription("Database state has changed, need to re-evaluate");
        }

        if (t instanceof FailedToAcquireLockException) {
            entryImpl.failed = false;
            log.setLevel("info");
            log.setDescription("Busy processing [" + ((FailedToAcquireLockException)t).getLockId() + "] will try later");
        }

        if (t instanceof ProcessExecutionExitException) {
            if (((ProcessExecutionExitException) t).getExitReason() == ExitReason.STATE_CHANGED) {
                entryImpl.failed = false;
                log.setLevel("info");
                log.setDescription("State has changed, need to re-evaluate");
            }
        }

        if (t instanceof TimeoutException) {
            entryImpl.failed = false;
            log.setLevel("info");
            Instance instance = objectManager.loadResource(Instance.class, log.getInstanceId());
            if (instance != null) {
                String error = TransitioningUtils.getTransitioningErrorMessage(instance);
                if (StringUtils.isNotBlank(error)) {
                    log.setLevel("error");
                    log.setDescription(log.getDescription() + ": " + error);
                }
            }
        }

        create(log);
    }

    protected void create(ServiceLog serviceLog) {
        String desc = serviceLog.getDescription();
        if (desc != null && desc.length() > 1024) {
            serviceLog.setDescription(desc.substring(0, 1024));
        }
        objectManager.create(serviceLog);
        ObjectUtils.publishChanged(eventService, objectManager, serviceLog);
    }

    protected Long getDeploymentIdFromThrowable(Throwable t) {
        Object obj = null;
        if (t instanceof ResourceTimeoutException) {
            obj = ((ResourceTimeoutException) t).getResource();
        }

        if (obj instanceof DeploymentUnit) {
            return ((DeploymentUnit) obj).getId();
        }
        return null;
    }

    protected Long getInstanceIdFromThrowable(Throwable t) {
        Object obj = null;
        if (t instanceof ResourceTimeoutException) {
            obj = ((ResourceTimeoutException) t).getResource();
        }
        if (obj instanceof Instance) {
            return ((Instance) obj).getId();
        }
        return null;
    }

    @Override
    public void waiting() {
        EntryImpl entry = entries.peek();
        entry.waiting = true;
    }

}
