package io.cattle.platform.process.externalevent;

import static io.cattle.platform.core.model.tables.EnvironmentTable.*;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.Environment;
import io.cattle.platform.core.model.ExternalEvent;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataUtils;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class ExternalServiceEventCreate extends AbstractDefaultProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(ExternalServiceEventCreate.class);

    @Inject
    ServiceDao serviceDao;
    @Inject
    ResourceMonitor resourceMonitor;
    @Inject
    GenericResourceDao resourceDao;
    @Inject
    LockManager lockManager;
    @Inject
    DynamicSchemaDao schemaDao;

    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final ExternalEvent event = (ExternalEvent)state.getResource();

        if (!ExternalEventConstants.KIND_SERVICE_EVENT.equals(event.getKind())) {
            return null;
        }

        lockManager.lock(new ExternalEventLock(SERVICE_LOCK_NAME, event.getAccountId(), event.getExternalId()), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Map<String, Object> serviceData = CollectionUtils.toMap(DataUtils.getFields(event).get(FIELD_SERVICE));
                if (serviceData.isEmpty()) {
                    log.warn("Empty service for externalServiceEvent: {}.", event);
                    return;
                }

                String kind = serviceData.get(ObjectMetaDataManager.KIND_FIELD) != null ? serviceData.get(ObjectMetaDataManager.KIND_FIELD).toString() : null;
                if (StringUtils.isEmpty(kind) || schemaDao.getSchema(kind, event.getAccountId(), null) == null) {
                    log.warn("Couldn't find schema for service type [{}]. Returning.", kind);
                    return;
                }

                if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_CREATE)) {
                    createService(event, serviceData);
                } else if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_UPDATE)) {
                    updateService(event, serviceData);
                } else if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_DELETE)) {
                    deleteService(event, serviceData);
                }
            }
        });
        return null;
    }

    void createService(ExternalEvent event, Map<String, Object> serviceData) {
        Service svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        if (svc != null) {
            return;
        }

        Environment environment = getEnvironment(event);
        if (environment == null) {
            log.info("Can't process service event. Could not get or create environment. Event: [{}]", event);
            return;
        }

        Map<String, Object> service = new HashMap<String, Object>();
        service.put(ObjectMetaDataManager.ACCOUNT_FIELD, event.getAccountId());
        service.put(FIELD_ENVIRIONMENT_ID, environment.getId());
        if (serviceData != null) {
            service.putAll(serviceData);
        }

        try {
            String create = objectProcessManager.getStandardProcessName(StandardProcess.CREATE, Service.class);
            String activate = objectProcessManager.getStandardProcessName(StandardProcess.ACTIVATE, Service.class);
            ProcessUtils.chainInData(service, create, activate);
            resourceDao.createAndSchedule(Service.class, service);
        } catch (ProcessCancelException e) {
            log.info("Create and activate process cancelled for service with account id {}and external id {}", 
                    event.getAccountId(), event.getExternalId());
        }
    }

    Environment getEnvironment(ExternalEvent event) {
        Map<String, Object> env = CollectionUtils.castMap(DataUtils.getFields(event).get(FIELD_ENVIRIONMENT));
        Object eId = CollectionUtils.getNestedValue(env, FIELD_EXTERNAL_ID);
        if (eId == null) {
            return null;
        }
        String envExtId = eId.toString();

        Environment environment = objectManager.findOne(Environment.class, ENVIRONMENT.EXTERNAL_ID, envExtId, ENVIRONMENT.REMOVED, null);
        if (environment == null) {
            final Environment newEnv = objectManager.newRecord(Environment.class);

            Object possibleName = CollectionUtils.getNestedValue(env, "name");
            newEnv.setExternalId(envExtId);
            newEnv.setAccountId(event.getAccountId());
            String name = possibleName != null ? possibleName.toString() : envExtId;
            newEnv.setName(name);

            environment = DeferredUtils.nest(new Callable<Environment>() {
                @Override
                public Environment call() {
                    return resourceDao.createAndSchedule(newEnv);
                }
            });

            environment = resourceMonitor.waitFor(environment, new ResourcePredicate<Environment>() {
                @Override
                public boolean evaluate(Environment obj) {
                    return obj != null && CommonStatesConstants.ACTIVE.equals(obj.getState());
                }
            });
        }
        return environment;
    }

    void updateService(ExternalEvent event, Map<String, Object> serviceData) {
        Service svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        if (svc == null) {
            log.info("Unable to find service while attempting to update. Returning. Service external id: [{}], account id: [{}]", event.getExternalId(),
                    event.getAccountId());
            return;
        }

        Map<String, Object> fields = DataUtils.getFields(svc);
        Map<String, Object> updates = new HashMap<String, Object>();
        for (Map.Entry<String, Object> resourceField : serviceData.entrySet()) {
            String fieldName = resourceField.getKey();
            Object newFieldValue = resourceField.getValue();
            Object currentFieldValue = fields.get(fieldName);
            if (ObjectUtils.hasWritableProperty(svc, fieldName)) {
                Object property = ObjectUtils.getProperty(svc, fieldName);
                if (newFieldValue != null && !newFieldValue.equals(property) || property == null) {
                    updates.put(fieldName, newFieldValue);
                }
            } else if ((newFieldValue != null && !newFieldValue.equals(currentFieldValue)) || currentFieldValue != null) {
                updates.put(fieldName, newFieldValue);
            }
        }

        if (!updates.isEmpty()) {
            objectManager.setFields(svc, updates);
            resourceDao.updateAndSchedule(svc);
        }
    }

    void deleteService(ExternalEvent event, Map<String, Object> serviceData) {
        Service svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        if (svc != null) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, svc, null);
        }
    }

    String getSelector(ExternalEvent event) {
        Object s = DataUtils.getFields(event).get("selector");
        String selector = s != null ? s.toString() : null;
        return selector;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { ExternalEventConstants.KIND_EXTERNAL_EVENT + ".create" };
    }
}
