package io.cattle.platform.liquibase;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.datasource.DataSourceFactory;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class Loader {

    private static final Set<SQLDialect> EMBEDDED = EnumSet.of(SQLDialect.H2, SQLDialect.HSQLDB, SQLDialect.DERBY);
    private static final String lockTable = "DATABASECHANGELOGLOCK";

    private static final DynamicStringProperty RELEASE_LOCK = ArchaiusUtil.getString("db.release.change.lock");
    private static final Logger log = LoggerFactory.getLogger("ConsoleStatus");

    Configuration configuration;
    DataSourceFactory dataSourceFactory;
    String changeLog;

    public Loader(Configuration configuration, DataSourceFactory dataSourceFactory, String changeLog) {
        super();
        this.configuration = configuration;
        this.dataSourceFactory = dataSourceFactory;
        this.changeLog = changeLog;
        init();
    }

    public void init() {
        DataSource dataSource = dataSourceFactory.createDataSource("liquibase");

        try {
            try {
                boolean release = false;
                if ("true".equals(RELEASE_LOCK.get())) {
                    release = true;
                } else if ("false".equals(RELEASE_LOCK.get())) {
                    release = false;
                } else {
                    release = EMBEDDED.contains(configuration.dialect());
                }

                if (release) {
                    DSL.using(configuration).delete(DSL.table(lockTable)).execute();
                }
            } catch (Throwable t) {
                // ignore errors
            }

            log.info("Starting DB migration");
            Connection c = null;
            Liquibase liquibase = null;
            try {
            log.info("1 Starting DB migration");
                c = dataSource.getConnection();
            log.info("2 Starting DB migration");
                liquibase = createLiquibase(c);
            log.info("3 Starting DB migration");
                liquibase.update((String)null);
            log.info("4 Starting DB migration");
            } catch (SQLException|LiquibaseException e) {
                throw new IllegalStateException("Failed to migrate DB", e);
            } finally {
                if (c != null) {
                    try {
                        if (!c.getAutoCommit()) {
                            c.rollback();
                        }
                    } catch (SQLException e) {
                        // nothing to do
                    }
                    try {
                        c.close();
                    } catch (SQLException e) {
                        // nothing to do
                    }
                }
            }

            log.info("DB migration done");
        } finally {
        }
    }

    protected Liquibase createLiquibase(Connection c) throws LiquibaseException {
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
        Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
        return liquibase;
    }

}