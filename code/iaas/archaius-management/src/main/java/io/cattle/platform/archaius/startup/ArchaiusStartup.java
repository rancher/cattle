package io.cattle.platform.archaius.startup;

import io.cattle.platform.archaius.polling.RefreshableFixedDelayPollingScheduler;
import io.cattle.platform.archaius.sources.LazyJDBCSource;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.datasource.DataSourceFactory;
import io.cattle.platform.extension.impl.ExtensionManagerImpl;
import io.cattle.platform.util.type.InitializationTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.MapConfiguration;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.sources.JDBCConfigurationSource;

public class ArchaiusStartup implements InitializationTask {

    public static final String CONFIG_KEY = "config";
    public static final String DB_CONFIG = "DatabaseConfig";

    private static Properties GLOBAL_DEFAULT = null;

    ExtensionManagerImpl extensionManager;
    ConcurrentCompositeConfiguration baseConfig;
    DataSource configDataSource;
    DataSourceFactory dataSourceFactory;
    String dataSourceName = "config";
    String query = "SELECT distinct name, value FROM setting";
    String keyColumnName = "name";
    String valueColumnName = "value";
    List<RefreshableFixedDelayPollingScheduler> schedulers;

    @PostConstruct
    public void init() {
        if ( GLOBAL_DEFAULT == null ) {
            throw new IllegalStateException("setGlobalDefaults() must be set before init() is called");
        }

        baseConfig = new ConcurrentCompositeConfiguration();
        baseConfig.addConfiguration(new MapConfiguration(getOverride()));
        baseConfig.addConfiguration(new MapConfiguration(GLOBAL_DEFAULT));

        DynamicPropertyFactory.initWithConfigurationSource(baseConfig);
    }

    protected Map<String,Object> getOverride() {
        Map<String,Object> override = new HashMap<String, Object>();
        override.put(CONFIG_KEY + ".exclude", DB_CONFIG);

        return override;
    }

    @Override
    public void start() {
        load(false);
        extensionManager.reset();
        load(true);
    }

    protected void load(boolean refresh) {
        List<AbstractConfiguration> configs = extensionManager.getExtensionList(CONFIG_KEY, AbstractConfiguration.class);

        for ( AbstractConfiguration config : configs ) {
            config.setDelimiterParsingDisabled(true);
        }

        if ( refresh ) {
            for ( AbstractConfiguration config : configs ) {
                refresh(config);
            }
        }

        baseConfig.clear();
        for ( AbstractConfiguration config : configs ) {
            baseConfig.addConfiguration(config);
        }

        if ( refresh ) {
            for ( RefreshableFixedDelayPollingScheduler scheduler : schedulers ) {
                scheduler.refresh();
            }

            ArchaiusUtil.addSchedulers(schedulers);
        }
    }

    protected void refresh(AbstractConfiguration config) {
        if ( config instanceof DynamicConfiguration && ((DynamicConfiguration)config).getSource() instanceof LazyJDBCSource ) {
            LazyJDBCSource source = (LazyJDBCSource) ((DynamicConfiguration)config).getSource();

            if ( configDataSource == null ) {
                configDataSource = dataSourceFactory.createDataSource(dataSourceName);
            }

            source.setSource(new JDBCConfigurationSource(configDataSource, query, keyColumnName, valueColumnName));
        }
    }

    @Override
    public void stop() {
    }

    public ExtensionManagerImpl getExtensionManager() {
        return extensionManager;
    }

    @Inject
    public void setExtensionManager(ExtensionManagerImpl extensionManager) {
        this.extensionManager = extensionManager;
    }

    public static Properties setGlobalDefaults(Properties props) {
        return GLOBAL_DEFAULT = props;
    }

    public static Properties getGlobalDefaults() {
        return GLOBAL_DEFAULT;
    }

    public DataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    @Inject
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getKeyColumnName() {
        return keyColumnName;
    }

    public void setKeyColumnName(String keyColumnName) {
        this.keyColumnName = keyColumnName;
    }

    public String getValueColumnName() {
        return valueColumnName;
    }

    public void setValueColumnName(String valueColumnName) {
        this.valueColumnName = valueColumnName;
    }

    public List<RefreshableFixedDelayPollingScheduler> getSchedulers() {
        return schedulers;
    }

    @Inject
    public void setSchedulers(List<RefreshableFixedDelayPollingScheduler> schedulers) {
        this.schedulers = schedulers;
    }

}
