package io.cattle.platform.object.postinit;

import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

public class SpecialFieldsPostInstantiationHandler implements ObjectPostInstantiationHandler {

    public static final String CREATED = "created";
    public static final String STATE = "state";
    public static final String KIND = "kind";

    SchemaFactory schemaFactory;

    public SpecialFieldsPostInstantiationHandler(SchemaFactory schemaFactory) {
        super();
        this.schemaFactory = schemaFactory;
    }

    @Override
    public <T> T postProcess(T obj, Class<T> clz, Map<String, Object> properties) {
        set(obj, CREATED, new Date());
        set(obj, STATE, "requested");

        Schema schema = schemaFactory.getSchema(clz);
        if (schema != null) {
            set(obj, KIND, schema.getId());
        }

        for (String key : new String[]{ CREATED, STATE, KIND}) {
            if (properties.containsKey(key) && properties.get(key) == null) {
                properties.remove(key);
            }
        }

        return obj;
    }

    protected void set(Object obj, String property, Object value) {
        try {
            if (BeanUtils.getProperty(obj, property) == null) {
                BeanUtils.setProperty(obj, property, value);
            }
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        }
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

}
