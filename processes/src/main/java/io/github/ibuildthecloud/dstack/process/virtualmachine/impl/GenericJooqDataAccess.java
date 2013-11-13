package io.github.ibuildthecloud.dstack.process.virtualmachine.impl;

import java.lang.reflect.InvocationTargetException;

import javax.inject.Inject;

import org.apache.commons.beanutils.PropertyUtils;
import org.jooq.UpdatableRecord;

import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;
import io.github.ibuildthecloud.dstack.db.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.dstack.process.virtualmachine.GenericObjectDataAccess;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

public class GenericJooqDataAccess extends AbstractJooqDao implements GenericObjectDataAccess {

    SchemaFactory schemaFactory;

    @Override
    public Object setState(Object obj, String fieldName, String state) {
        UpdatableRecord<?> record = getRecord(obj);
        try {
            PropertyUtils.setProperty(record, fieldName, state);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set field [" + fieldName + "] to [" + state + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to set field [" + fieldName + "] to [" + state + "]", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to set field [" + fieldName + "] to [" + state + "]", e);
        }

        record.update();
        record.refresh();
        return record;
    }

    @Override
    public String getState(Object obj, String fieldName) {
        try {
            Object result = PropertyUtils.getProperty(obj, fieldName);
            return result == null ? null : result.toString();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to get field [" + fieldName + "]", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to get field [" + fieldName + "]", e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to get field [" + fieldName + "]", e);
        }
    }

    @Override
    public Object load(String resourceType, String id) {
        Class<UpdatableRecord<?>> record = getRecord(schemaFactory.getSchemaClass(resourceType));
        return JooqUtils.findById(create(), record, id);
    }

    @SuppressWarnings("unchecked")
    protected Class<UpdatableRecord<?>> getRecord(Class<?> clz) {
        if ( ! UpdatableRecord.class.isAssignableFrom(clz) )
            throw new IllegalArgumentException("Excepted UpdatableRecord but got [" + clz + "]");
        return (Class<UpdatableRecord<?>>)clz;
    }
    protected UpdatableRecord<?> getRecord(Object obj) {
        if ( obj instanceof UpdatableRecord<?> ) {
            return (UpdatableRecord<?>)obj;
        }
        throw new IllegalArgumentException("Excepted UpdatableRecord but got [" + obj + "]");
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}
