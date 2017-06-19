package io.cattle.platform.app.components;

import io.cattle.platform.archaius.polling.RefreshableFixedDelayPollingScheduler;
import io.cattle.platform.archaius.sources.DefaultTransformedEnvironmentProperties;
import io.cattle.platform.archaius.sources.LazyJDBCSource;
import io.cattle.platform.archaius.sources.NamedDynamicConfiguration;
import io.cattle.platform.archaius.sources.NamedMapConfiguration;
import io.cattle.platform.archaius.sources.NamedSystemConfiguration;
import io.cattle.platform.archaius.sources.OptionalPropertiesConfigurationFactory;
import io.cattle.platform.archaius.sources.TransformedEnvironmentProperties;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.datasource.DataSourceFactory;
import io.cattle.platform.datasource.JMXDataSourceFactoryImpl;
import io.cattle.platform.db.jooq.logging.LoggerListener;
import io.cattle.platform.liquibase.Loader;
import io.cattle.platform.logback.Startup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.configuration.AbstractConfiguration;
import org.jooq.Configuration;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.jooq.tools.StopWatchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.sources.JDBCConfigurationSource;

public class Bootstrap {

    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    private static final String[] DEFAULTS = new String[] {
            "META-INF/cattle/agent-server/defaults.properties",
            "META-INF/cattle/system-services/defaults.properties",
            "META-INF/cattle/defaults/defaults.properties",
            "META-INF/cattle/system/defaults.properties",
            "META-INF/cattle/redis/defaults.properties",
            "META-INF/cattle/core-model/defaults.properties",
            "META-INF/cattle/process/defaults.properties",
            "META-INF/cattle/encryption/defaults.properties",
            "META-INF/cattle/allocator-server/defaults.properties",
            "META-INF/cattle/core-object-defaults/defaults.properties",
            "META-INF/cattle/bootstrap/defaults.properties",
            "META-INF/cattle/iaas-api/defaults.properties",
            "META-INF/cattle/config-defaults/defaults.properties",
            "META-INF/cattle/api-server/defaults.properties",
            "META-INF/cattle/defaults/dev-defaults.properties",
            "META-INF/cattle/system-services/healthcheck-defaults.properties",
            "META-INF/cattle/service-upgrade/defaults.properties",
        };

    DataSourceFactory dataSourceFactory = new JMXDataSourceFactoryImpl();
    DataSource dataSource;
    Configuration jooqConfig;
    ConcurrentCompositeConfiguration baseConfig = new ConcurrentCompositeConfiguration();
    RefreshableFixedDelayPollingScheduler scheduler = new RefreshableFixedDelayPollingScheduler();
    LazyJDBCSource dbConfigSource = new LazyJDBCSource();

    public Bootstrap() throws IOException {
        setHomeAndEnv();
        setupArchaius();
        setupLogging();
        setupDatabase();
        //migrateSchema();
    }

    private void setHomeAndEnv() throws IOException {
        String home = System.getenv("CATTLE_HOME");
        if (home == null) {
            home = System.getProperty("cattle.home");
        }
        if (home == null) {
            home = new File(".", "runtime").getAbsolutePath();
        }

        if (!home.endsWith(File.separator)) {
            home = home + File.separator;
        }

        System.setProperty("cattle.home", home);

        File homeFile = new File(home);
        if (!homeFile.exists() && !homeFile.mkdirs()) {
            throw new IOException("Failed to create [" + homeFile.getAbsolutePath() + "]");
        }

        if (System.getProperty("logback.bootstrap.level") == null) {
            System.setProperty("logback.bootstrap.level", "WARN");
        }

        CONSOLE_LOG.info("[BOOTSTRAP] CATTLE_HOME=", homeFile.getAbsolutePath());
    }


    private void setupArchaius() throws IOException {
        ArchaiusUtil.setScheduler(scheduler);

        getSources().forEach((s) -> {
            s.setDelimiterParsingDisabled(true);
            baseConfig.addConfiguration(s);
        });

        DynamicPropertyFactory.initWithConfigurationSource(baseConfig);

        dbConfigSource.setSource(new JDBCConfigurationSource(
                dataSourceFactory.createDataSource("config"),
                "SELECT distinct name, value FROM setting",
                "name",
                "value"));

        ArchaiusUtil.refresh();
    }

    private void setupLogging() {
        Startup startup = new Startup();
        startup.init();
    }

    private void setupDatabase() {
        dataSource = dataSourceFactory.createDataSource("cattle");

        DataSourceConnectionProvider dscp = new DataSourceConnectionProvider(dataSource);
        ThreadLocalTransactionProvider tp = new ThreadLocalTransactionProvider(dscp, false);

        LoggerListener logger = new LoggerListener();
        logger.setMaxLength(1000);

        io.cattle.platform.db.jooq.config.Configuration config = new io.cattle.platform.db.jooq.config.Configuration();
        config.setName("cattle");
        config.setConnectionProvider(dscp);
        config.setTransactionProvider(tp);
        config.setListeners(Arrays.asList(
                logger,
                new StopWatchListener()));

        jooqConfig = config;
    }

    private void migrateSchema() {
        new Loader(jooqConfig, dataSourceFactory, "db/changelog.xml");
    }

    private List<AbstractConfiguration> getSources() throws IOException {
        return Arrays.asList(
                new TransformedEnvironmentProperties(),
                new NamedSystemConfiguration(),
                OptionalPropertiesConfigurationFactory.getConfiguration("cattle-local.properties"),
                new NamedDynamicConfiguration(dbConfigSource, scheduler, "Database"),
                OptionalPropertiesConfigurationFactory.getConfiguration("cattle.properties"),
                new DefaultTransformedEnvironmentProperties(),
                OptionalPropertiesConfigurationFactory.getConfiguration("cattle-override.properties"),
                OptionalPropertiesConfigurationFactory.getConfiguration("cattle-global.properties"),
                new NamedMapConfiguration(globalProps(), "Code Packaged Defaults"));
    }

    private Properties globalProps() throws IOException {
        Properties props = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        for (String config : DEFAULTS) {
            try (InputStream is = cl.getResourceAsStream(config)) {
                if (is != null) {
                    props.load(is);
                }
            }
        }

        return props;
    }

}
