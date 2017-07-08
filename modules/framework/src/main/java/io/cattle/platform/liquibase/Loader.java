package io.cattle.platform.liquibase;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.datasource.DataSourceFactory;
import liquibase.Liquibase;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.parser.core.xml.XMLChangeLogSAXParser;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;

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
            Liquibase liquibase;
            try {
                c = dataSource.getConnection();
                liquibase = createLiquibase(c);
                liquibase.update((String)null);
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

            log.info("Finished DB migration");
        } finally {
        }
    }

    protected Liquibase createLiquibase(Connection c) throws LiquibaseException {
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
		DatabaseChangeLog parsedLog = new Parser().parse(changeLog, new ChangeLogParameters(database), new ClassLoaderResourceAccessor());
        Liquibase liquibase = new Liquibase(parsedLog, new ClassLoaderResourceAccessor(), database);
        return liquibase;
    }

    private static class Parser extends XMLChangeLogSAXParser {
        public Parser() {
            getSaxParserFactory().setNamespaceAware(false);
            getSaxParserFactory().setValidating(false);
        }
    }

}