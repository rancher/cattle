package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.Map;

import javax.inject.Inject;

public class GenericResourceDaoImpl implements GenericResourceDao {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    @Override
    public <T> T createAndSchedule(Class<T> clz, Map<String, Object> properties) {
        T obj = objectManager.create(clz, properties);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, properties);

        return objectManager.reload(obj);
    }

    @Override
    public <T> T createAndSchedule(Class<T> clz, Object key, Object... values) {
        Map<Object,Object> properties = CollectionUtils.asMap(key, values);
        return createAndSchedule(clz, objectManager.convertToPropertiesFor(clz, properties));
    }

    @Override
    public <T> T createAndSchedule(T obj, Map<String, Object> processData) {
        obj = objectManager.create(obj);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, processData);
        return objectManager.reload(obj);
    }

    @Override
    public <T> T createAndSchedule(T o) {
        T obj = objectManager.create(o);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, null);
        return objectManager.reload(o);
    }

    @Override
    public <T> T updateAndSchedule(T o) {
        T obj = objectManager.persist(o);
        processManager.scheduleStandardProcess(StandardProcess.UPDATE, obj, null);
        return objectManager.reload(o);
    }

    @Override
    public <T> T updateAndSchedule(T o, Map<String, Object> fields) {
        o = objectManager.reload(o);
        T obj = objectManager.setFields(o, fields);
        processManager.scheduleStandardProcess(StandardProcess.UPDATE, obj, null);
        return objectManager.reload(o);
    }

    @Override
    public <T> T create(Class<T> clz, Map<String, Object> properties) {
        T obj = objectManager.create(clz, properties);
        processManager.executeStandardProcess(StandardProcess.CREATE, obj, properties);

        return objectManager.reload(obj);
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
