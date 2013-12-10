package io.github.ibuildthecloud.dstack.datasource;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import javax.sql.DataSource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDataSourceFactoryImpl implements DataSourceFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataSourceFactoryImpl.class);

    public static Object setConfig(Object poolOrConfig, String dataSourceName, String... prefixes) {

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
                        log.info("Setting Pool [{}] property [{}={}]", dataSourceName, desc.getName(), "****");
                    } else {
                        log.info("Setting Pool [{}] property [{}={}]", dataSourceName, desc.getName(), newValue);
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

    protected static String getProperty(String key) {
        return ArchaiusUtil.getStringProperty(key).get();
    }

    @Override
    public DataSource createDataSource(String name) {
        String server = getProperty("db." + name + ".database");
        String alias = getProperty("db." + name + ".alias");

        if ( server == null && alias != null ) {
            server = getProperty("db." + alias + ".database");
        }

        BasicDataSource ds = new BasicDataSource();
        if ( alias == null ) {
            setConfig(ds, name, String.format("db.%s.%s.", name, server), String.format("db.%s.", name), "db.");
        } else {
            setConfig(ds, name, String.format("db.%s.%s.", name, server), String.format("db.%s.", name),
                    String.format("db.%s.%s.", alias, server), String.format("db.%s.", alias), "db.");
        }

        return ds;
    }

}
