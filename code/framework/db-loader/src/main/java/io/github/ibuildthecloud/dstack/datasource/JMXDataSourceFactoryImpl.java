package io.github.ibuildthecloud.dstack.datasource;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.ManagedBasicDataSource;

import com.netflix.config.DynamicStringProperty;

public class JMXDataSourceFactoryImpl extends DefaultDataSourceFactoryImpl {

    private static final DynamicStringProperty PREFIX = ArchaiusUtil.getString("dbcp.jmx.prefix");

    @Override
    protected BasicDataSource newBasicDataSource(String name) {
        return new ManagedBasicDataSource(PREFIX.get() + name);
    }

}
