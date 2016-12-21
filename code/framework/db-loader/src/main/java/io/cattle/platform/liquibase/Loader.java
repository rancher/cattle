package io.cattle.platform.liquibase;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.datasource.DataSourceFactory;

import java.util.EnumSet;
import java.util.Set;

import javax.inject.Inject;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class Loader extends SpringLiquibase {

    private static final Set<SQLDialect> EMBEDDED = EnumSet.of(SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.DERBY);

    private static final String LOG_OPTION = "liquibase.databaseChangeLogTableName";
    private static final String LOCK_OPTION = "liquibase.databaseChangeLogLockTableName";

    private static final DynamicStringProperty RELEASE_LOCK = ArchaiusUtil.getString("db.release.change.lock");
    private static final Logger log = LoggerFactory.getLogger("ConsoleStatus");

    Configuration configuration;
    String lockTable = "DATABASECHANGELOGLOCK";
    String changeLogTable = "DATABASECHANGELOG";
    DataSourceFactory dataSourceFactory;

    @Override
    public void afterPropertiesSet() throws LiquibaseException {
        if (getDataSource() == null && dataSourceFactory != null) {
            setDataSource(dataSourceFactory.createDataSource("liquibase"));
        }

        String oldLockTable = System.getProperty(LOCK_OPTION);
        String oldLogTable = System.getProperty(LOG_OPTION);
        try {
            try {
                System.setProperty(LOG_OPTION, changeLogTable);
                System.setProperty(LOCK_OPTION, lockTable);
                boolean release = false;
                if ("true".equals(RELEASE_LOCK.get())) {
                    release = true;
                } else if ("false".equals(RELEASE_LOCK.get())) {
                    release = false;
                } else {
                    release = EMBEDDED.contains(configuration.dialect());
                }

                if (release) {
                    DSL.using(getConfiguration()).delete(DSL.table(lockTable)).execute();
                }
            } catch (Throwable t) {
                // ignore errors
            }

            log.info("Starting DB migration");
            super.afterPropertiesSet();
            log.info("DB migration done");
        } finally {
            if (oldLogTable == null) {
                System.clearProperty(LOG_OPTION);
            } else {
                System.setProperty(LOG_OPTION, oldLogTable);
            }
            if (oldLockTable == null) {
                System.clearProperty(LOCK_OPTION);
            } else {
                System.setProperty(LOCK_OPTION, oldLockTable);
            }
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getLockTable() {
        return lockTable;
    }

    public void setLockTable(String lockTable) {
        this.lockTable = lockTable;
    }

    public String getChangeLogTable() {
        return changeLogTable;
    }

    public void setChangeLogTable(String changeLogTable) {
        this.changeLogTable = changeLogTable;
    }

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

}