package io.cattle.platform.datasource;

import io.cattle.platform.pool.PoolConfig;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;

public class DefaultDataSourceFactoryImpl implements DataSourceFactory {

    @Override
    public DataSource createDataSource(String name) {
        String server = PoolConfig.getProperty("db." + name + ".database");
        String alias = PoolConfig.getProperty("db." + name + ".alias");

        if (server == null && alias != null) {
            server = PoolConfig.getProperty("db." + alias + ".database");
        }

        BasicDataSource ds = newBasicDataSource(name);
        if (alias == null) {
            PoolConfig.setConfig(ds, name, String.format("db.%s.%s.", name, server), String.format("db.%s.", name), "db.", "global.pool.");
        } else {
            PoolConfig.setConfig(ds, name, String.format("db.%s.%s.", name, server), String.format("db.%s.", name), String.format("db.%s.%s.", alias, server),
                    String.format("db.%s.", alias), "db.", "global.pool.");
        }

        return ds;
    }

    protected BasicDataSource newBasicDataSource(String name) {
        return new BasicDataSource();
    }

}
