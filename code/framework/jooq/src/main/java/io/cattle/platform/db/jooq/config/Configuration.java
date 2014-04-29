package io.cattle.platform.db.jooq.config;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.ExecuteListener;
import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultExecuteListenerProvider;

public class Configuration extends DefaultConfiguration {

    private static final long serialVersionUID = -726368732372005280L;

    String name;
    DataSource dataSource;
    ConnectionProvider connectionProvider;
    List<ExecuteListener> listeners;
    Settings settings = new Settings();

    @PostConstruct
    public void init() {
        String prop = "db." + name + ".database";
        String database = ArchaiusUtil.getString(prop).get();
        if ( database == null ) {
            throw new IllegalStateException("Failed to find config for [" + prop + "]");
        }

        try {
            SQLDialect dialect = SQLDialect.valueOf(database.trim().toUpperCase());
            set(dialect);
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException("Invalid SQLDialect [" + database.toUpperCase() + "]", e);
        }

        if ( connectionProvider == null ) {
            set(new AutoCommitConnectionProvider(dataSource));
        } else {
            set(connectionProvider);
        }

        settings.setRenderSchema(false);

        String renderNameStyle = ArchaiusUtil.getString("db." + name + "." + database + ".render.name.style").get();
        if ( renderNameStyle != null ) {
            settings.setRenderNameStyle(RenderNameStyle.valueOf(renderNameStyle.trim().toUpperCase()));
        }

        set(settings);

        if ( listeners != null && listeners.size() > 0 ) {
            settings().setExecuteLogging(false);
            set(DefaultExecuteListenerProvider.providers(listeners.toArray(new ExecuteListener[listeners.size()])));
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getName() {
        return name;
    }

    @Inject
    public void setName(String name) {
        this.name = name;
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public void setConnectionProvider(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public List<ExecuteListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<ExecuteListener> listeners) {
        this.listeners = listeners;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

}