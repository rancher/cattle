package io.cattle.platform.process.driver;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.engine.handler.AbstractProcessLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.lock.DriverLock;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class DriverServiceActivate extends AbstractProcessLogic implements ProcessPreListener {

    public static String[] DRIVERS = new String[]{"networkDriver", "storageDriver"};

    @Inject
    JsonMapper jsonMapper;
    @Inject
    LockManager lockManager;
    @Inject
    ObjectManager objectManager;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public String[] getProcessNames() {
        return new String[]{"service.create", "service.update", "service.activate"};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        final Service service = (Service)state.getResource();

        for (final String driverKey : DRIVERS) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> driverMap = DataAccessor.fields(service).withKey(driverKey)
                    .withDefault(Collections.EMPTY_MAP).as(Map.class);
            if (driverMap.size() == 0) {
                continue;
            }
            Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
            if (stack == null) {
                continue;
            }
            if (ServiceConstants.isSystem(stack)) {
                lockManager.lock(new DriverLock(service, driverKey), new LockCallbackNoReturn() {
                    @Override
                    public void doWithLockNoResult() {
                        addDriver(driverKey, service, driverMap);
                    }
                });
            }
        }

        return null;
    }

    protected Object findDriver(String driverKey, Service service, Map<String, Object> fields) {
        Class<?> driverClass = objectManager.getSchemaFactory().getSchemaClass(driverKey);
        return objectManager.findAny(driverClass,
                "serviceId", service.getId(),
                ObjectMetaDataManager.REMOVED_FIELD, null);
    }

    protected void addDriver(String driverKey, Service service, Map<String, Object> fields) {
        Object driver = findDriver(driverKey, service, fields);
        if (driver == null) {
            driver = createDriver(driverKey, service, fields);
        }
        if (!CommonStatesConstants.ACTIVE.equals(ObjectUtils.getState(driver))) {
            processManager.scheduleStandardProcessAsync(StandardProcess.ACTIVATE, driver, null);
        }
    }

    protected String getString(Map<String, Object> fields, String field) {
        return DataAccessor.fromMap(fields).withKey(field).as(String.class);
    }

    private Object createDriver(String driverKey, Service service, Map<String, Object> fields) {
        Class<?> driverClass = objectManager.getSchemaFactory().getSchemaClass(driverKey);

        String name = getString(fields, ObjectMetaDataManager.NAME_FIELD);
        if (name == null) {
            name = service.getName();
        }

        Map<String, Object> data = CollectionUtils.asMap("fields", fields);
        return objectManager.create(driverClass,
                ObjectMetaDataManager.NAME_FIELD, name,
                ObjectMetaDataManager.ACCOUNT_FIELD, service.getAccountId(),
                "serviceId", service.getId(),
                ObjectMetaDataManager.DATA_FIELD, data);
    }
}