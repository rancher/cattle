package io.cattle.platform.app;

import io.cattle.platform.archaius.polling.RefreshableFixedDelayPollingScheduler;
import io.cattle.platform.archaius.sources.DefaultTransformedEnvironmentProperties;
import io.cattle.platform.archaius.sources.LazyJDBCSource;
import io.cattle.platform.archaius.sources.NamedDynamicConfiguration;
import io.cattle.platform.archaius.sources.NamedMapConfiguration;
import io.cattle.platform.archaius.sources.NamedSystemConfiguration;
import io.cattle.platform.archaius.sources.OptionalPropertiesConfigurationFactory;
import io.cattle.platform.archaius.sources.TransformedEnvironmentProperties;
import io.cattle.platform.archaius.startup.ArchaiusStartup;
import io.cattle.platform.datasource.DataSourceFactory;
import io.cattle.platform.extension.dynamic.DynamicExtensionManager;
import io.cattle.platform.extension.impl.EMUtils;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.logback.Startup;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class ConfigConfig {

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

    @Bean
    ArchaiusStartup ArchaiusStartup(ExtensionManagerImpl em, @Qualifier("GlobalProperties") Properties props,
            DataSourceFactory dsf, RefreshableFixedDelayPollingScheduler scheduler) {
        ArchaiusStartup.setGlobalDefaults(props);
        ArchaiusStartup startup = new ArchaiusStartup();
        startup.setExtensionManager(em);
        startup.setDataSourceFactory(dsf);
        startup.setSchedulers(Arrays.asList(scheduler));

        EMUtils.addConfig(em, new DefaultTransformedEnvironmentProperties(), "DefaultEnvironmentConfig");
        EMUtils.addConfig(em, new TransformedEnvironmentProperties(), "EnvironmentConfig");
        EMUtils.addConfig(em, new NamedSystemConfiguration(), "SystemConfig");

        AbstractConfiguration localFileConfig = OptionalPropertiesConfigurationFactory.getConfiguration("cattle-local.properties");
        EMUtils.addConfig(em, localFileConfig, "CattleLocalFileConfig");

        NamedDynamicConfiguration dbConfig = new NamedDynamicConfiguration(new LazyJDBCSource(), scheduler);
        dbConfig.setSourceName("Database");
        EMUtils.addConfig(em, dbConfig, "DatabaseConfig");

        AbstractConfiguration cattleFileConfig = OptionalPropertiesConfigurationFactory.getConfiguration("cattle.properties");
        EMUtils.addConfig(em, cattleFileConfig, "CattleFileConfig");

        AbstractConfiguration cattleOverrideFileConfig = OptionalPropertiesConfigurationFactory.getConfiguration("cattle-override.properties");
        EMUtils.addConfig(em, cattleOverrideFileConfig, "CattleOverrideFileConfig");

        AbstractConfiguration cattleGlobalFileConfig = OptionalPropertiesConfigurationFactory.getConfiguration("cattle-global.properties");
        EMUtils.addConfig(em, cattleGlobalFileConfig, "CattleGlobalFileConfig");

        NamedMapConfiguration defaultsConfig = new NamedMapConfiguration(props);
        defaultsConfig.setSourceName("Code Packaged Defaults");
        EMUtils.addConfig(em, defaultsConfig, "DefaultsConfig");

        startup.init();
        em.start();
        startup.start();

        return startup;
    }

    @Bean
    RefreshableFixedDelayPollingScheduler ConfigScheduler() {
        return new RefreshableFixedDelayPollingScheduler();
    }

    @Bean
    ExtensionManagerImpl extensionManager() {
        return new DynamicExtensionManager();
    }

    @Bean
    Properties GlobalProperties() throws IOException {
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

    @Bean
    @DependsOn("ArchaiusStartup")
    Startup Startup() {
        return new Startup();
    }

}
