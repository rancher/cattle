package io.cattle.platform.core.dao.impl;

import java.util.Map;

import javax.inject.Inject;

import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.util.type.CollectionUtils;

public class GenericResourceDaoImpl implements GenericResourceDao {

    ObjectManager objectManager;
    ObjectProcessManager processManager;

    @Override
    public <T> T createAndSchedule(Class<T> clz, Map<String, Object> properties) {
        T obj = objectManager.create(clz, properties);
        processManager.scheduleStandardProcess(StandardProcess.CREATE, obj, properties);

        return obj;
    }

    @Override
    public <T> T createAndSchedule(Class<T> clz, Object key, Object... values) {
        Map<Object,Object> properties = CollectionUtils.asMap(key, values);
        return createAndSchedule(clz, objectManager.convertToPropertiesFor(clz, properties));
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
