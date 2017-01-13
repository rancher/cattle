package io.cattle.platform.object.util;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.process.LaunchConfiguration;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Schema;

import java.util.HashMap;
import java.util.Map;

public class ObjectLaunchConfigurationUtils {

    public static LaunchConfiguration createConfig(SchemaFactory factory, String processName, Object resource, Map<String, Object> data) {
        Schema schema = factory.getSchema(resource.getClass());

        if (schema == null) {
            throw new IllegalArgumentException("Failed to find schema for [" + resource + "]");
        }

        Field field = schema.getResourceFields().get(ObjectMetaDataManager.ID_FIELD);

        if (field == null) {
            throw new IllegalStateException("Schema [" + schema.getId() + "] does not have an ID field so we can not launch a process for it");
        }

        Object id = field.getValue(resource);

        if (id == null) {
            throw new IllegalStateException("Object [" + resource + "] has a null ID");
        }

        String[] parts = processName.split("[.]");
        int priority = ArchaiusUtil.getInt("process." + processName + ".priority").get();
        if (priority == 0) {
            priority = ArchaiusUtil.getInt("process." + parts[parts.length-1] + ".priority").get();
        }
        if (priority == 0) {
            priority = ArchaiusUtil.getInt("process." + parts[0] + ".priority").get();
        }

        if (priority >= 0 && ObjectUtils.isSystem(resource)) {
            priority += 1000;
        }

        return new LaunchConfiguration(processName, schema.getId(), id.toString(), ObjectUtils.getAccountId(resource), priority,
                data == null ? new HashMap<String, Object>() : data);
    }

}
