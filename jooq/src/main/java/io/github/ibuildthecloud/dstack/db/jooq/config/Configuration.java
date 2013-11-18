package io.github.ibuildthecloud.dstack.db.jooq.config;

import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;

public class Configuration extends DefaultConfiguration {

    private static final long serialVersionUID = -726368732372005280L;

    String name;
    DataSource dataSource;

    @PostConstruct
    public void init() {
        String prop = "db." + name + ".database";
        String database = ArchaiusUtil.getStringProperty(prop).get();
        if ( database == null ) {
            throw new IllegalStateException("Failed to find config for [" + prop + "]");
        }

        try {
            SQLDialect dialect = SQLDialect.valueOf(database.toUpperCase());
            set(dialect);
        } catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException("Invalid SQLDialect [" + database.toUpperCase() + "]", e);
        }

        set(new AutoCommitConnectionProvider(dataSource));

        Settings settings = new Settings();
        settings.setRenderSchema(false);
        settings.setRenderNameStyle(RenderNameStyle.UPPER);
        set(settings);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Inject
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
}
