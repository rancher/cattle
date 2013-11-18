package io.github.ibuildthecloud.dstack.object.impl;

import io.github.ibuildthecloud.dstack.db.jooq.utils.JooqUtils;

import javax.inject.Inject;

import org.jooq.Configuration;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;

public class JooqObjectManager extends AbstractObjectManager {

    Configuration configuration;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T instantiate(Class<T> clz, Object properties) {
        UpdatableRecord<?> record = JooqUtils.getRecord(clz);
        record.attach(getConfiguration());

        return (T)record;
    }

    @Override
    public <T> T insert(T instance, Class<T> clz, Object properties) {
        JooqUtils.getRecordObject(instance).insert();
        return instance;
    }

    @Override
    public <T> T reload(T obj) {
        UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
        record.attach(getConfiguration());
        record.refresh();
        return obj;
    }

    @Override
    public <T> T persist(T obj) {
        UpdatableRecord<?> record = JooqUtils.getRecordObject(obj);
        record.attach(getConfiguration());
        record.update();
        return obj;
    }

    @Override
    public <T> T loadResource(String resourceType, String resourceId) {
        return loadResource(resourceType, (Object)resourceId);
    }

    public <T> T loadResource(String resourceType, Long resourceId) {
        return loadResource(resourceType, (Object)resourceId);
    }

    @SuppressWarnings("unchecked")
    protected <T> T loadResource(String resourceType, Object resourceId) {
        Class<?> clz = schemaFactory.getSchemaClass(resourceType);
        return (T) JooqUtils.findById(DSL.using(getConfiguration()), clz, resourceId);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
