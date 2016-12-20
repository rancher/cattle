package io.cattle.platform.sample.data;

import static io.cattle.platform.core.model.tables.DataTable.*;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.dao.AccountDao;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.core.model.Data;
import io.cattle.platform.engine.process.impl.ProcessCancelException;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.sample.lock.SampleDataLock;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.InitializationTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;

public abstract class AbstractSampleData implements InitializationTask {

    private static final Logger log = LoggerFactory.getLogger(SampleDataStartupV3.class);

    private static final DynamicBooleanProperty RUN = ArchaiusUtil.getBoolean("sample.setup");

    protected ObjectManager objectManager;
    protected ObjectProcessManager processManager;
    protected AccountDao accountDao;
    protected JsonMapper jsonMapper;
    protected LockManager lockManager;

    @Override
    public final void start() {
        if (!RUN.get()) {
            return;
        }

        lockManager.lock(new SampleDataLock(), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                startWithLock();
            }
        });
    }

    protected final void startWithLock() {
        if (!RUN.get()) {
            return;
        }
        String name = getName();

        Data data = objectManager.findAny(Data.class, DATA.NAME, name);

        if (data != null) {
            return;
        }

        Account system = accountDao.getSystemAccount();
        if (system == null) {
            log.warn("Failed to find system account, not populating system data");
            return;
        }

        List<Object> toCreate = new ArrayList<Object>();

        populatedData(system, toCreate);

        for (Object object : toCreate) {
            try {
                processManager.executeStandardProcess(StandardProcess.CREATE, object, null);
            } catch (ProcessCancelException e) {
            }
        }

        objectManager.create(Data.class, DATA.NAME, name, DATA.VALUE, "true");
    }

    protected abstract String getName();

    protected abstract void populatedData(Account system, List<Object> toCreate);

    protected <T> T createByUuid(Class<T> type, String uuid, Object key, Object... values) {
        Map<Object, Object> inputProperties = CollectionUtils.asMap(key, values);
        inputProperties.put(ObjectMetaDataManager.UUID_FIELD, uuid);
        Map<String, Object> properties = objectManager.convertToPropertiesFor(type, inputProperties);

        T existing = objectManager.findAny(type, ObjectMetaDataManager.UUID_FIELD, uuid);
        if (existing != null) {
            objectManager.setFields(existing, properties);
            return existing;
        }

        return objectManager.create(type, properties);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AccountDao getAccountDao() {
        return accountDao;
    }

    @Inject
    public void setAccountDao(AccountDao accountDao) {
        this.accountDao = accountDao;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

}