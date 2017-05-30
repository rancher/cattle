package io.cattle.platform.core.dao.impl;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;

@Named
public class GenericMapDaoImpl extends AbstractCoreDao implements GenericMapDao {

    SchemaFactory schemaFactory;
    ObjectMetaDataManager metaDataManager;

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<? extends T> findNonRemoved(Class<T> mapType, Class<?> resourceType, long resourceId) {
        String type = schemaFactory.getSchemaName(mapType);

        Table<?> table = getTable(mapType);

        Relationship reference = getRelationship(mapType, resourceType);
        TableField<?, Object> removed = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.REMOVED_FIELD);
        TableField<?, Object> referenceField = JooqUtils.getTableField(metaDataManager, type, reference.getPropertyName());

        if ( removed == null || referenceField == null ) {
            throw new IllegalArgumentException("Type [" + mapType + "] is missing required removed or reference column");
        }

        return (List<? extends T>)create()
                .selectFrom(table)
                .where(
                        removed.isNull()
                        .and(referenceField.eq(resourceId)))
                .fetch();
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T findNonRemoved(Class<T> mapType, Class<?> leftResourceType, long leftResourceId,
            Class<?> rightResourceType, long rightResourceId) {
        String type = schemaFactory.getSchemaName(mapType);

        Table<?> table = getTable(mapType);

        Relationship leftReference = getRelationship(mapType, leftResourceType);
        Relationship rightReference = getRelationship(mapType, rightResourceType);
        TableField<?, Object> removed = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.REMOVED_FIELD);
        TableField<?, Object> leftReferenceField = JooqUtils.getTableField(metaDataManager, type, leftReference.getPropertyName());
        TableField<?, Object> rightReferenceField = JooqUtils.getTableField(metaDataManager, type, rightReference.getPropertyName());

        if ( removed == null || leftReferenceField == null || rightReferenceField == null ) {
            throw new IllegalArgumentException("Type [" + mapType + "] is missing required removed or references column");
        }

        return (T)create()
                .selectFrom(table)
                .where(
                        removed.isNull()
                        .and(leftReferenceField.eq(leftResourceId))
                        .and(rightReferenceField.eq(rightResourceId)))
                .fetchOne();
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> List<? extends T> findToRemove(Class<T> mapType, Class<?> resourceType, long resourceId) {
        String type = schemaFactory.getSchemaName(mapType);

        Table<?> table = getTable(mapType);

        Relationship reference = getRelationship(mapType, resourceType);
        TableField<?, Object> removed = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.REMOVED_FIELD);
        TableField<?, Object> referenceField = JooqUtils.getTableField(metaDataManager, type, reference.getPropertyName());
        TableField<?, Object> state = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.STATE_FIELD);

        if ( removed == null || referenceField == null || state == null ) {
            throw new IllegalArgumentException("Type [" + mapType + "] is missing required removed, reference, or state column");
        }

        return (List<? extends T>)create()
                .selectFrom(table)
                .where(
                        referenceField.eq(resourceId)
                        .and(
                                removed.isNull()
                                .or(state.eq(CommonStatesConstants.REMOVING))))
                .fetch();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<? extends T> findAll(Class<T> mapType, Class<?> resourceType, long resourceId) {
        String type = schemaFactory.getSchemaName(mapType);

        Table<?> table = getTable(mapType);

        Relationship reference = getRelationship(mapType, resourceType);
        TableField<?, Object> referenceField = JooqUtils.getTableField(metaDataManager, type, reference.getPropertyName());
        TableField<?, Object> state = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.STATE_FIELD);

        if ( referenceField == null || state == null ) {
            throw new IllegalArgumentException("Type [" + mapType + "] is missing required reference or state column");
        }

        return (List<? extends T>)create()
                .selectFrom(table)
                .where(
                referenceField.eq(resourceId))
                .fetch();
    }

    protected <T> Table<?> getTable(Class<?> mapType) {
        Class<UpdatableRecord<?>> record = JooqUtils.getRecordClass(schemaFactory, mapType);
        return JooqUtils.getTableFromRecordClass(record);
    }

    protected Relationship getRelationship(Class<?> mapType, Class<?> resourceType) {
        Map<String,Relationship> rels = metaDataManager.getLinkRelationships(schemaFactory, schemaFactory.getSchemaName(mapType));
        Relationship reference = null;
        for ( Map.Entry<String,Relationship> entry : rels.entrySet() ) {
            Relationship rel = entry.getValue();
            if ( rel.getRelationshipType() == Relationship.RelationshipType.REFERENCE && resourceType.isAssignableFrom(rel.getObjectType())) {
                reference = rel;
                break;
            }
        }

        if ( reference == null ) {
            throw new IllegalArgumentException("Failed to find reference relationship from [" + mapType + "] to [" + resourceType + "]");
        }

        return reference;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    @Named("CoreSchemaFactory")
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T findToRemove(Class<T> mapType, Class<?> leftResourceType, long leftResourceId,
            Class<?> rightResourceType, long rightResourceId) {
        String type = schemaFactory.getSchemaName(mapType);

        Table<?> table = getTable(mapType);

        Relationship leftReference = getRelationship(mapType, leftResourceType);
        Relationship rightReference = getRelationship(mapType, rightResourceType);
        TableField<?, Object> removed = JooqUtils.getTableField(metaDataManager, type,
                ObjectMetaDataManager.REMOVED_FIELD);
        TableField<?, Object> state = JooqUtils.getTableField(metaDataManager, type, ObjectMetaDataManager.STATE_FIELD);
        TableField<?, Object> leftReferenceField = JooqUtils.getTableField(metaDataManager, type,
                leftReference.getPropertyName());
        TableField<?, Object> rightReferenceField = JooqUtils.getTableField(metaDataManager, type,
                rightReference.getPropertyName());

        if (removed == null || leftReferenceField == null || rightReferenceField == null) {
            throw new IllegalArgumentException("Type [" + mapType
                    + "] is missing required removed or references column");
        }

        return (T) create()
                .selectFrom(table)
                .where(
                        (removed.isNull().or(state.eq(CommonStatesConstants.REMOVING)))
                                .and(leftReferenceField.eq(leftResourceId))
                                .and(rightReferenceField.eq(rightResourceId)))
                .fetchOne();
    }

}
