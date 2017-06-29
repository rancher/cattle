package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.util.TransactionDelegate;

import java.util.Map;

public class GenericResourceDaoImpl implements GenericResourceDao {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    TransactionDelegate transaction;

    public GenericResourceDaoImpl(ObjectManager objectManager, ObjectProcessManager processManager, TransactionDelegate transaction) {
        super();
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.transaction = transaction;
    }

    @Override
    public <T> T createAndSchedule(Class<T> clz, Map<String, Object> properties) {
        return transaction.doInTransactionResult(() -> {
            T obj = objectManager.create(clz, properties);
            processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, properties);

            return objectManager.reload(obj);
        });
    }

    @Override
    public <T> T createAndSchedule(Class<T> clz, Object key, Object... values) {
        Map<Object,Object> properties = CollectionUtils.asMap(key, values);
        return createAndSchedule(clz, objectManager.convertToPropertiesFor(clz, properties));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T createAndSchedule(T objIn, Map<String, Object> processData) {
        return transaction.doInTransactionResult(() -> {
            Object obj = objectManager.create(objIn);
            processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, processData);
            return (T)objectManager.reload(obj);
        });
    }

    @Override
    public <T> T createAndSchedule(T o) {
        return transaction.doInTransactionResult(() -> {
            T obj = objectManager.create(o);
            processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, null);
            return objectManager.reload(o);
        });
    }

    @Override
    public <T> T updateAndSchedule(T o) {
        return transaction.doInTransactionResult(() -> {
            T obj = objectManager.persist(o);
            processManager.scheduleStandardProcess(StandardProcess.UPDATE, obj, null);
            return objectManager.reload(o);
        });
    }

    @Override
    public <T> T updateAndSchedule(T oIn, Map<String, Object> fields) {
        return transaction.doInTransactionResult(() -> {
            Object o = objectManager.reload(oIn);
            T obj = objectManager.setFields(o, fields);
            processManager.scheduleStandardProcess(StandardProcess.UPDATE, obj, null);
            return objectManager.reload(oIn);
        });
    }

    @Override
    public <T> T create(Class<T> clz, Object key, Object... values) {
        Map<Object,Object> properties = CollectionUtils.asMap(key, values);
        return create(clz, objectManager.convertToPropertiesFor(clz, properties));
    }

    @Override
    public <T> T create(Class<T> clz, Map<String, Object> properties) {
        return transaction.doInTransactionResult(() -> {
            T obj = objectManager.create(clz, properties);
            processManager.executeStandardProcess(StandardProcess.CREATE, obj, properties);

            return objectManager.reload(obj);
        });
    }
}
