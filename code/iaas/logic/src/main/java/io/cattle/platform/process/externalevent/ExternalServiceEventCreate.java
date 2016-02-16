package io.cattle.platform.process.externalevent;

import static io.cattle.platform.core.constants.InstanceConstants.PROCESS_DATA_NO_OP;
import static io.cattle.platform.core.model.tables.EnvironmentTable.ENVIRONMENT;
import static io.cattle.platform.core.model.tables.ServiceTable.SERVICE;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.FIELD_ENVIRIONMENT;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.FIELD_ENVIRIONMENT_ID;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.FIELD_EXTERNAL_ID;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.FIELD_SERVICE;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.FIELD_UUID;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.SERVICE_LOCK_NAME;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.TYPE_SERVICE_CREATE;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.TYPE_SERVICE_DELETE;
import static io.cattle.platform.process.externalevent.ExternalEventConstants.TYPE_SERVICE_UPDATE;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.DynamicSchemaDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.ServiceDao;
import io.cattle.platform.core.model.DynamicSchema;
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
import io.cattle.platform.object.util.DataAccessor;
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
                        Map<String, Object> serviceData = CollectionUtils.toMap(DataUtils.getFields(event).get(
                                FIELD_SERVICE));
                        if (serviceData.isEmpty()) {
                            log.warn("Empty service for externalServiceEvent: {}.", event);
                            return;
                        }

                        String kind = serviceData.get(ObjectMetaDataManager.KIND_FIELD) != null ? serviceData.get(
                                ObjectMetaDataManager.KIND_FIELD).toString() : null;
                        DynamicSchema schema = schemaDao.getSchema(kind, event.getAccountId(), null);
                        if (StringUtils.isEmpty(kind) || schema == null) {
                            log.warn("Couldn't find schema for service type [{}]. Returning.", kind);
                            return;
                        }

                        Service svc = getExistingService(event, serviceData);

                        if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_CREATE)) {
                            if (svc == null) {
                                createService(event, serviceData);
                            } else {
                                updateService(svc, event, serviceData);
                            }
                        } else if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_UPDATE)) {
                            updateService(svc, event, serviceData);
                        } else if (StringUtils.equals(event.getEventType(), TYPE_SERVICE_DELETE)) {
                            deleteService(svc, event, serviceData);
                        }
            }
        });
        return null;
    }

    private Service getExistingService(ExternalEvent event, Map<String, Object> serviceData) {
        Object uuidObj = serviceData.get(FIELD_UUID);
        Service svc = null;
        if (uuidObj != null) {
            svc = objectManager.findOne(Service.class, SERVICE.UUID, uuidObj.toString(), SERVICE.REMOVED, null);
        } else {
            svc = serviceDao.getServiceByExternalId(event.getAccountId(), event.getExternalId());
        }

        if (svc != null) {
            return svc;
        }
        return null;
    }

    void createService(ExternalEvent event, Map<String, Object> serviceData) {
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
            Service svc = objectManager.create(Service.class, service);
            objectProcessManager.scheduleStandardProcess(StandardProcess.CREATE, svc, makeData(service));
        } catch (ProcessCancelException e) {
            log.info("Create and activate process cancelled for service with account id {}and external id {}", 
                    event.getAccountId(), event.getExternalId());
        }
    }

    Environment getEnvironment(ExternalEvent event) {
        Map<String, Object> env = CollectionUtils.castMap(DataUtils.getFields(event).get(FIELD_ENVIRIONMENT));
        Object eId = CollectionUtils.getNestedValue(env, FIELD_EXTERNAL_ID);
        Object uuidObj = CollectionUtils.getNestedValue(env, FIELD_UUID);
        if (eId == null && uuidObj == null) {
            return null;
        }
        Environment environment;
        String envExtId = eId.toString();
        if (uuidObj != null) {
            String uuid = uuidObj.toString();
            environment = objectManager.findOne(Environment.class, ENVIRONMENT.UUID, uuid,
                    ENVIRONMENT.REMOVED,
                    null);
        } else {
            environment = objectManager.findOne(Environment.class, ENVIRONMENT.EXTERNAL_ID, envExtId,
                    ENVIRONMENT.REMOVED, null);
        }

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

    void updateService(Service svc, ExternalEvent event, Map<String, Object> serviceData) {
        if (svc == null) {
            log.info("Unable to find service while attempting to update. Returning. Service external id: [{}], account id: [{}]", event.getExternalId(),
                    event.getAccountId());
            return;
        }

        Map<String, Object> serviceDataFields = new HashMap<>();
        Map<String, Object> serviceFields = new HashMap<>();
        for (Map.Entry<String, Object> resourceField : serviceData.entrySet()) {
            String fieldName = resourceField.getKey();
            Object newFieldValue = resourceField.getValue();
            if (!fieldName.equals("data")) {
                serviceFields.put(fieldName, newFieldValue);
            } else {
                Map<String, Object> data = CollectionUtils.toMap(newFieldValue);
                Map<String, Object> dataFields = CollectionUtils.toMap(data.get("fields"));
                serviceDataFields.putAll(dataFields);
            }
        }
        
        Map<String, Object> fields = DataUtils.getFields(svc);
        Map<String, Object> serviceDataUpdates = getUpdates(svc, fields, serviceDataFields);
        Map<String, Object> serviceUpdates = getUpdates(svc, fields, serviceFields);


        if (!serviceDataUpdates.isEmpty()) {
            DataUtils.getWritableFields(svc).putAll(serviceDataUpdates);
        }
        if (!serviceUpdates.isEmpty()) {
            svc = objectManager.setFields(svc, serviceUpdates);
        }
        svc = objectManager.persist(svc);
        if (svc.getState().equals(CommonStatesConstants.ACTIVE)) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.UPDATE, svc,
                    makeData(new HashMap<String, Object>()));
        } else {
            objectProcessManager.scheduleStandardProcess(StandardProcess.ACTIVATE, svc,
                    makeData(new HashMap<String, Object>()));
        }

    }

    protected Map<String, Object> makeData(Map<String, Object> data) {
        DataAccessor.fromMap(data).withKey(PROCESS_DATA_NO_OP).set(true);
        return data;
    }

    protected Map<String, Object> getUpdates(Service svc, Map<String, Object> fields,
            Map<String, Object> serviceFields) {
        Map<String, Object> updates = new HashMap<>();
        for (Map.Entry<String, Object> resourceField : serviceFields.entrySet()) {
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
        return updates;
    }

    void deleteService(Service svc, ExternalEvent event, Map<String, Object> serviceData) {
        if (svc != null) {
            objectProcessManager.scheduleStandardProcess(StandardProcess.REMOVE, svc,
                    makeData(new HashMap<String, Object>()));
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
