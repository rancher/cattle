package io.github.ibuildthecloud.dstack.datasource;

import javax.sql.DataSource;

public interface DataSourceFactory {

    DataSource createDataSource(String name);

}
