package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.db.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJooqResourceManager implements ResourceManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultJooqResourceManager.class);

    Configuration configuration;
    DataSource dataSource;
    SchemaFactory schemaFactory;

    @PostConstruct
    public void init() {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.MYSQL);
        configuration.set(new DataSourceConnectionProvider(dataSource));

        this.configuration = configuration;
    }

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }


    @Override
    public Object getById(String id, ApiRequest request) {
        Class<?> clz = schemaFactory.getSchemaClass(request.getType());
        if ( clz == null ) {
            return null;
        }

        return JooqUtils.findById(create(), clz, id);
    }

    @Override
    public Object list(String type, ApiRequest request) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        if ( clz == null ) {
            return null;
        }

        Table<?> table = getTable(clz);
        if ( table == null )
            return null;

        return create().select().from(table).fetchInto(clz);
    }

    @SuppressWarnings("unchecked")
    protected Table<?> getTable(Class<?> clz) {
        if ( UpdatableRecord.class.isAssignableFrom(clz) ) {
            try {
                UpdatableRecord<?> record =
                        ((Class<UpdatableRecord<?>>)clz).newInstance();
                return record.getTable();
            } catch (InstantiationException e) {
                log.error("Failed to determine table for [{}]", clz, e);
            } catch (IllegalAccessException e) {
                log.error("Failed to determine table for [{}]", clz, e);
            }
        }
        return null;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    @Inject
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

}
