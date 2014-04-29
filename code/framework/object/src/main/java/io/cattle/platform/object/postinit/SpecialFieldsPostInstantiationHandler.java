package io.cattle.platform.object.postinit;

import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;

public class SpecialFieldsPostInstantiationHandler implements ObjectPostInstantiationHandler, Priority {

    public static final String UUID = "uuid";
    public static final String ZONE_ID = "zoneId";
    public static final String CREATED = "created";
    public static final String STATE = "state";
    public static final String KIND = "kind";

    SchemaFactory schemaFactory;

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        set(obj, UUID, java.util.UUID.randomUUID().toString());
        set(obj, CREATED, new Date());
        set(obj, STATE, "requested");

        Schema schema = schemaFactory.getSchema(clz);
        if ( schema != null ) {
            set(obj, KIND, schema.getId());
        }

        //TODO Bad!
        set(obj, ZONE_ID, 1L);
        return obj;
    }

    protected void set(Object obj, String property, Object value) {
        try {
            if ( BeanUtils.getProperty(obj, property) == null ) {
                BeanUtils.setProperty(obj, property, value);
            }
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    @Override
    public int getPriority() {
        return Priority.PRE;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}
