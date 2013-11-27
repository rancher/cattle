package io.github.ibuildthecloud.dstack.object.util;

import io.github.ibuildthecloud.dstack.engine.process.LaunchConfiguration;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.HashMap;
import java.util.Map;

public class ObjectLaunchConfigurationUtils {

    public static LaunchConfiguration createConfig(SchemaFactory factory, String processName, Object resource, Map<String,Object> data) {
        Schema schema = factory.getSchema(resource.getClass());

        if ( schema == null ) {
            throw new IllegalArgumentException("Failed to find schema for [" + resource + "]");
        }

        Field field = schema.getResourceFields().get("id");

        if ( field == null ) {
            throw new IllegalStateException("Schema [" + schema.getId() + "] does not have an ID field so we can not launch a process for it");
        }

        Object id = field.getValue(resource);

        if ( id == null ) {
            throw new IllegalStateException("Object [" + resource + "] does has a null ID");
        }

        return new LaunchConfiguration(processName, schema.getId(), id.toString(), data == null ? new HashMap<String, Object>() : data);
    }

    public static LaunchConfiguration createConfig(SchemaFactory factory, String processName, Object resource) {
        return createConfig(factory, processName, resource, new HashMap<String, Object>());
    }

    public static LaunchConfiguration createConfig(SchemaFactory factory, String processName, Object resource, String key, Object... value) {
        Map<String,Object> data = new HashMap<String, Object>();

        if ( value.length == 0 || (value.length % 2 == 0) ) {
            throw new IllegalArgumentException("Invalid number of arguments, you must pass key1, value1, key2, value2, etc");
        }

        data.put(key, value[0]);
        for ( int i = 1 ; i+1 < value.length; i += 1 ) {
            data.put(value[i].toString(), value[i+1]);
        }

        return createConfig(factory, processName, resource, data);
    }
}
