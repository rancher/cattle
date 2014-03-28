package io.cattle.platform.agent.server.resource.dao.impl;

import io.cattle.platform.agent.server.resource.dao.MappedResourceChangeHandlerDao;
import io.cattle.platform.agent.server.resource.impl.MissingDependencyException;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.RecordHandler;
import org.jooq.Table;
import org.jooq.TableField;

public class MappedResourceChangeHandlerDaoImpl extends AbstractJooqDao implements MappedResourceChangeHandlerDao {

    ObjectManager objectManager;
    ObjectProcessManager processManager;
    SchemaFactory schemaFactory;
    GenericResourceDao resourceDao;

    @SuppressWarnings("unchecked")
    @Override
    public Set<String> getMappedUuids(String uuid, Class<?> resourceType, Class<?> mappingType, Class<?> otherType) {
        Table<?> resourceTable = JooqUtils.getTable(schemaFactory, resourceType);
        Table<?> mappingTable = JooqUtils.getTable(schemaFactory, mappingType);
        Table<?> otherTable = JooqUtils.getTable(schemaFactory, otherType);
        TableField<Record,Field<Long>> resourceTableField = getReferenceField(mappingTable, resourceTable);
        TableField<Record,Field<Long>> otherTableField = getReferenceField(mappingTable, otherTable);

        final Set<String> result = new HashSet<String>();

        create()
            .select((Field<String>)otherTable.field(ObjectMetaDataManager.UUID_FIELD))
            .from(resourceTable)
            .join(mappingTable)
                .on(resourceTableField.eq((Field<Long>)resourceTable.field(ObjectMetaDataManager.ID_FIELD)))
            .join(otherTable)
                .on(otherTableField.eq((Field<Long>)otherTable.field(ObjectMetaDataManager.ID_FIELD)))
            .where(
                    ((Field<String>)resourceTable.field(ObjectMetaDataManager.UUID_FIELD)).eq(uuid))
            .fetchInto(new RecordHandler<Record1<String>>() {
                @Override
                public void next(Record1<String> record) {
                    result.add(record.value1());
                }
            });

        return result;
    }

    @Override
    public <T> T newResource(Class<T> resourceType, Class<?> mappingType, Class<?> otherType,
            Map<String, Object> properties, String mappedUuid) throws MissingDependencyException {
        Table<?> resourceTable = JooqUtils.getTable(schemaFactory, resourceType);
        Table<?> mappingTable = JooqUtils.getTable(schemaFactory, mappingType);
        Table<?> otherTable = JooqUtils.getTable(schemaFactory, otherType);
        TableField<Record,Field<Long>> resourceTableField = getReferenceField(mappingTable, resourceTable);
        TableField<Record,Field<Long>> otherTableField = getReferenceField(mappingTable, otherTable);

        if ( mappedUuid == null ) {
            throw new IllegalStateException("Expected a mapped resource");
        }

        Object otherResource = objectManager.findOne(otherType,
                ObjectMetaDataManager.UUID_FIELD, mappedUuid);

        if ( otherResource == null ) {
            throw new MissingDependencyException();
        }

        T resource = resourceDao.createAndSchedule(resourceType, properties);

        Map<Object,Object> fields = new HashMap<Object, Object>();
        fields.put(resourceTableField, ObjectUtils.getId(resource));
        fields.put(otherTableField, ObjectUtils.getId(otherResource));

        Map<String,Object> mappingResource = objectManager.convertToPropertiesFor(mappingType, fields);

        resourceDao.createAndSchedule(mappingType, mappingResource);

        return resource;
    }


    @SuppressWarnings("unchecked")
    protected TableField<Record, Field<Long>> getReferenceField(Table<?> mappingTable, Table<?> otherTable) {
        List<?> foreignKeys = mappingTable.getReferencesTo(otherTable);
        if ( foreignKeys.size() != 1 ) {
            throw new IllegalStateException("Expected one foreign key from [" + mappingTable +
                    "] to [" + otherTable + "]");
        }

        List<TableField<Record, ?>> fields = ((ForeignKey<Record, Record>)foreignKeys.get(0)).getFields();
        if ( fields.size() != 1 ) {
            throw new IllegalStateException("Expected on field for foreign key [" + foreignKeys.get(0) + "]");
        }

        return (TableField<Record, Field<Long>>)fields.get(0);
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

}
