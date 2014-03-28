package io.cattle.platform.datasource;

import javax.sql.DataSource;

public interface DataSourceFactory {

    DataSource createDataSource(String name);

}
