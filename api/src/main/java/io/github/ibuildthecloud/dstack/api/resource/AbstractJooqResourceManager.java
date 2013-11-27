package io.github.ibuildthecloud.dstack.api.resource;

import io.github.ibuildthecloud.dstack.api.utils.ApiUtils;
import io.github.ibuildthecloud.dstack.object.ObjectManager;
import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DefaultDSLContext;

public abstract class AbstractJooqResourceManager implements ResourceManager {

//    private static final Logger log = LoggerFactory.getLogger(AbstractJooqResourceManager.class);

    Configuration configuration;
    DataSource dataSource;
    SchemaFactory schemaFactory;
    ObjectManager objectManager;

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    @Override
    public Object create(String type, ApiRequest request) {
        Class<?> clz = schemaFactory.getSchemaClass(request.getType());
        if ( clz == null ) {
            return null;
        }

        Class<UpdatableRecord<?>> recordClass = JooqUtils.getRecordClass(schemaFactory, clz);
        UpdatableRecord<?> record = objectManager.create(recordClass,
                ApiUtils.getMap(request.getRequestObject()));

        return record;
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

        Table<?> table = JooqUtils.getTable(clz);
        if ( table == null )
            return null;

        return create().select().from(table).fetchInto(clz);
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

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

}
