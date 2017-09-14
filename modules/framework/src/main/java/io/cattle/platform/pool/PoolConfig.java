package io.cattle.platform.pool;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolConfig {

    private static final Logger log = LoggerFactory.getLogger(PoolConfig.class);

    private static final String JMX_NAME = "jmxNamePrefix";
    private static final Map<String, String[]> TRY_ALSO = new HashMap<String, String[]>();

    static {
        TRY_ALSO.put("maxTotal", new String[] { "maxActive" });
        TRY_ALSO.put("maxActive", new String[] { "maxTotal" });

    }

    protected static String getValue(String name, String... prefixes) {
        for (String prefix : prefixes) {
            String newValue = getProperty(prefix + name.toLowerCase());
            if (newValue != null)
                return newValue;
        }

        return null;
    }

    public static Object setConfig(Object poolOrConfig, String poolName, String... prefixes) {

        for (PropertyDescriptor desc : org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptors(poolOrConfig)) {
            if (desc.getWriteMethod() == null)
                continue;

            String newValue = getValue(desc.getName(), prefixes);

            if (newValue == null && TRY_ALSO.containsKey(desc.getName())) {
                for (String otherKey : TRY_ALSO.get(desc.getName())) {
                    newValue = getValue(otherKey, prefixes);
                    if (newValue != null) {
                        break;
                    }
                }
            }

            if (newValue == null && desc.getName().equals(JMX_NAME)) {
                newValue = poolName;
            }

            if (newValue != null) {
                try {
                    if ("password".equals(desc.getName())) {
                        log.info("Setting Pool [{}] property [{}={}]", poolName, desc.getName(), "****");
                    } else {
                        log.info("Setting Pool [{}] property [{}={}]", poolName, desc.getName(), newValue);
                    }
                    BeanUtils.setProperty(poolOrConfig, desc.getName(), newValue);
                } catch (IllegalAccessException e) {
                    log.error("Failed to set property [{}] on db config", desc.getName(), e);
                } catch (InvocationTargetException e) {
                    log.error("Failed to set property [{}] on db config", desc.getName(), e);
                }
            }
        }

        return poolOrConfig;
    }

    public static String getProperty(String key) {
        return ArchaiusUtil.getString(key).get();
    }

}
