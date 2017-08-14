package io.cattle.platform.process.driver;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.StorageDriverConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.StoragePoolDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.core.model.StorageDriver;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.engine.handler.HandlerResult;
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
import io.cattle.platform.storage.service.StorageService;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.cattle.platform.core.model.tables.StorageDriverTable.*;

public class DriverProcessManager {
    public static String[] DRIVERS = new String[]{"networkDriver", "storageDriver"};

    JsonMapper jsonMapper;
    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    GenericResourceDao resourceDao;
    StoragePoolDao storagePoolDao;
    StorageService storageService;

    public DriverProcessManager(JsonMapper jsonMapper, LockManager lockManager, ObjectManager objectManager, ObjectProcessManager processManager,
            GenericResourceDao resourceDao, StoragePoolDao storagePoolDao, StorageService storageService) {
        super();
        this.jsonMapper = jsonMapper;
        this.lockManager = lockManager;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.resourceDao = resourceDao;
        this.storagePoolDao = storagePoolDao;
        this.storageService = storageService;
    }

    public HandlerResult activate(ProcessState state, ProcessInstance process) {
        final Service service = (Service)state.getResource();

        for (final String driverKey : DRIVERS) {
            final Map<String, Object> driverMap = DataAccessor.fieldMapRO(service, driverKey);
            if (driverMap.size() == 0) {
                continue;
            }
            Stack stack = objectManager.loadResource(Stack.class, service.getStackId());
            if (stack == null) {
                continue;
            }
            if (canCreateDriver(stack)) {
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

    protected boolean canCreateDriver(Stack stack) {
        Account account = objectManager.loadResource(Account.class, stack.getAccountId());
        if (account == null) {
            return false;
        }

        return account.getClusterOwner() && Objects.equals(account.getClusterId(), stack.getClusterId());
    }

    public HandlerResult remove(ProcessState state, ProcessInstance process) {
        for (String driverKey : DRIVERS) {
            Class<?> driverClass = objectManager.getSchemaFactory().getSchemaClass(driverKey);

            Service service = (Service)state.getResource();
            for (Object driver : objectManager.children(service, driverClass)) {
                processManager.remove(driver, null);
            }
        }
        return null;
    }

    public HandlerResult storageDriverActivate(ProcessState state, ProcessInstance process) {
        StorageDriver storageDriver = (StorageDriver)state.getResource();
        String scope = DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_SCOPE);
        if (!StorageDriverConstants.VALID_SCOPES.contains(scope)) {
            scope = StorageDriverConstants.SCOPE_ENVIRONMENT;
        }
        String accessMode = DataAccessor.fieldString(storageDriver, StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE);
        if (!VolumeConstants.VALID_ACCESS_MODES.contains(accessMode)) {
            accessMode = VolumeConstants.DEFAULT_ACCESS_MODE;
        }

        List<String> capabilities = DataAccessor.fieldStringList(storageDriver, ObjectMetaDataManager.CAPABILITIES_FIELD);
        if (StorageDriverConstants.SCOPE_LOCAL.equals(scope)) {
            if(!new HashSet<>(capabilities).contains(StorageDriverConstants.CAPABILITY_SCHEDULE_SIZE)) {
                capabilities.add(StorageDriverConstants.CAPABILITY_SCHEDULE_SIZE);
            }
        }

        return new HandlerResult(StorageDriverConstants.FIELD_SCOPE, scope,
                StorageDriverConstants.FIELD_VOLUME_ACCESS_MODE, accessMode,
                ObjectMetaDataManager.CAPABILITIES_FIELD, capabilities);
    }

    public HandlerResult storageDriverRemove(ProcessState state, ProcessInstance process) {
        StorageDriver driver = (StorageDriver)state.getResource();
        for (StoragePool pool : storagePoolDao.findNonRemovedStoragePoolByDriver(driver.getId())) {
            processManager.deactivateThenRemove(pool, null);
        }
        return null;
    }

    public HandlerResult setupPools(ProcessState state, ProcessInstance process) {
        Object resource = state.getResource();
        if (resource instanceof Host) {
           List<StorageDriver> drivers = objectManager.find(StorageDriver.class,
                   STORAGE_DRIVER.CLUSTER_ID, ((Host) resource).getClusterId(),
                   STORAGE_DRIVER.REMOVED, null);
           for (StorageDriver driver : drivers) {
               storageService.setupPools(driver);
           }
        } else if (resource instanceof StorageDriver) {
            storageService.setupPools((StorageDriver)resource);
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
                ObjectMetaDataManager.CLUSTER_FIELD, service.getClusterId(),
                "serviceId", service.getId(),
                ObjectMetaDataManager.DATA_FIELD, data);
    }

}
