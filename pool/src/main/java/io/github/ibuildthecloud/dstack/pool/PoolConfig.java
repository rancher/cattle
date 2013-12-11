package io.github.ibuildthecloud.dstack.pool;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolConfig {

    private static final Logger log = LoggerFactory.getLogger(PoolConfig.class);

    public static Object setConfig(Object poolOrConfig, String poolName, String... prefixes) {

        for ( PropertyDescriptor desc : org.apache.commons.beanutils.PropertyUtils.getPropertyDescriptors(poolOrConfig) ) {
            if ( desc.getWriteMethod() == null )
                continue;

            String newValue = null;
            for ( String prefix : prefixes ) {
                newValue = getProperty(prefix + desc.getName().toLowerCase());
                if ( newValue != null )
                    break;
            }

            if ( newValue != null ) {
                try {
                    if ( "password".equals(desc.getName()) ) {
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
        return ArchaiusUtil.getStringProperty(key).get();
    }

}
