package io.github.ibuildthecloud.dstack.object.meta.impl;

import static io.github.ibuildthecloud.dstack.object.meta.Relationship.RelationshipType.*;
import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.meta.TypeSet;
import io.github.ibuildthecloud.dstack.util.init.AfterExtensionInitialization;
import io.github.ibuildthecloud.dstack.util.init.InitializationUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.Field.Type;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.model.impl.FieldImpl;
import io.github.ibuildthecloud.model.impl.SchemaImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.Column;

import org.apache.commons.beanutils.PropertyUtils;
import org.jooq.ForeignKey;
import org.jooq.Table;
import org.jooq.TableField;

public class DefaultObjectMetaDataManager implements ObjectMetaDataManager, SchemaPostProcessor {

    SchemaFactory schemaFactory;
    List<TypeSet> typeSets;
    Map<Class<?>,Map<String,Relationship>> relationships = new HashMap<Class<?>, Map<String,Relationship>>();

    Map<FieldCacheKey, String> propertyCache = new WeakHashMap<FieldCacheKey, String>();
    Map<FieldCacheKey, TableField<?, ?>> tableFields = new HashMap<FieldCacheKey, TableField<?,?>>();
    Map<String,Set<String>> linksCache = new WeakHashMap<String, Set<String>>();

    @AfterExtensionInitialization
    public void postInit() {
        List<Schema> schemas = registerTypes();
        registerRelationships();
        parseSchemas(schemas);
    }

    protected void registerRelationships() {
        for ( TypeSet typeSet : typeSets ) {
            for ( Class<?> clz : typeSet.getTypeClasses() ) {
                Table<?> table = JooqUtils.getTable(clz);
                if ( table == null ) {
                    continue;
                }

                registerTableFields(table);
                for ( ForeignKey<?, ?> reference : table.getReferences() ) {
                    register(reference);
                }
            }
        }
    }

    protected void registerTableFields(Table<?> table) {
        Class<?> clz = table.getClass();
        for ( java.lang.reflect.Field field : clz.getFields() ) {
            if ( TableField.class.isAssignableFrom(field.getType()) && Modifier.isPublic(field.getModifiers()) ) {
                try {
                    field.setAccessible(true);
                    TableField<?, ?> tableField = (TableField<?, ?>) field.get(table);
                    String name = getNameFromField(table.getRecordType(), tableField.getName());
                    tableFields.put(new FieldCacheKey(table.getRecordType(), name), tableField);
                } catch (IllegalArgumentException e) {
                    throw new IllegalStateException(e);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    protected List<Schema> registerTypes() {
        schemaFactory.addPostProcessor(this);
        List<Schema> schemas = new ArrayList<Schema>();

        for ( TypeSet typeSet : typeSets ) {
            for ( String name : typeSet.getTypeNames() ) {
                schemas.add(schemaFactory.registerSchema(name));
            }
            for ( Class<?> type : typeSet.getTypeClasses() ) {
                schemas.add(schemaFactory.registerSchema(type));
            }
        }

        return schemas;
    }

    protected void parseSchemas(List<Schema> schemas) {
        for ( Schema schema : schemas ) {
            schemaFactory.parseSchema(schema.getId());
        }
    }

    protected void register(ForeignKey<?, ?> foreignKey) {
        TableField<?, ?>[] fields = foreignKey.getFieldsArray();
        if ( fields.length == 0 || fields.length > 1 ) {
            return;
        }
        TableField<?, ?> field = fields[0];

        String propertyName = getNameFromField(field.getTable().getRecordType(), field.getName());
        String referenceName = propertyName;
        if ( field.getName().endsWith(ID_FIELD) ) {
            referenceName = referenceName.substring(0, referenceName.length() - 2);
        }

        Class<?> localType = foreignKey.getTable().getRecordType();
        Class<?> foreignType = foreignKey.getKey().getTable().getRecordType();

        Schema localSchema = schemaFactory.getSchema(localType);
        String childName = localSchema.getPluralName();

        register(localType, new ForeignKeyRelationship(REFERENCE, referenceName, propertyName, foreignType, foreignKey));
        register(foreignType, new ForeignKeyRelationship(CHILD, childName, propertyName, localType, foreignKey));
    }

    protected void register(Class<?> type, Relationship relationship) {
        Map<String,Relationship> relationships = this.relationships.get(type);
        if ( relationships == null ) {
            relationships = new HashMap<String, Relationship>();
            this.relationships.put(type, relationships);
        }

        relationships.put(relationship.getName().toLowerCase(), relationship);
    }


    @Override
    public Object convertFieldNameFor(String type, Object key) {
        if ( key instanceof TableField<?, ?> )
            return key;

        Class<?> clz = schemaFactory.getSchemaClass(type);
        FieldCacheKey cacheKey = new FieldCacheKey(clz, key.toString());

        return tableFields.get(cacheKey);
    }

    @Override
    public String convertPropertyNameFor(Class<?> recordClass, Object key) {
        if ( key instanceof String ) {
            return (String)key;
        }

        if ( key instanceof TableField ) {
            TableField<?, ?> field = (TableField<?, ?>)key;
            return getNameFromField(recordClass, field.getName());
        }

        return key == null ? null : key.toString();
    }

    protected String getNameFromField(Class<?> clz, String field) {
        FieldCacheKey key = new FieldCacheKey(clz, field);
        String cached = propertyCache.get(key);

        if ( cached != null )
            return cached;

        for ( PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(clz) ) {
            Method readMethod = desc.getReadMethod();
            Method writeMethod = desc.getWriteMethod();

            if ( readMethod == null || writeMethod == null ) {
                continue;
            }

            Column column = readMethod.getAnnotation(Column.class);
            if ( column != null && field.equals(column.name()) ) {
                propertyCache.put(key, desc.getName());
                return desc.getName();
            }
        }

        throw new IllegalArgumentException("Failed to find bean property for table field [" + field + "] on [" + clz + "]");
    }

    @Override
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactoryImpl factory) {
        return schema;
    }

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactoryImpl factory) {
        Map<String,Relationship> relationships = this.relationships.get(factory.getSchemaClass(schema.getId()));
        if ( relationships == null ) {
            return schema;
        }

        for ( Relationship relationship : relationships.values() ) {
            if ( relationship.getRelationshipType() != REFERENCE )
                continue;

            Field field = schema.getResourceFields().get(relationship.getPropertyName());
            if ( ! ( field instanceof FieldImpl ) ) {
                continue;
            }

            FieldImpl fieldImpl = (FieldImpl)field;
            fieldImpl.setType(null);
            fieldImpl.setTypeEnum(Type.REFERENCE);
            fieldImpl.setSubType(factory.getSchema(relationship.getObjectType()).getId());
        }

        return schema;
    }


    @Override
    public Set<String> getLinks(SchemaFactory schemaFactory, Resource resource) {
        String key = schemaFactory.getId() + "-" + resource.getType();
        Set<String> links = linksCache.get(key);
        if ( links != null )
            return links;

        links = new TreeSet<String>();
        Schema schema = schemaFactory.getSchema(resource.getType());

        Map<String,Relationship> relationships = this.relationships.get(schemaFactory.getSchemaClass(schema.getId()));
        if ( relationships == null || relationships.size() == 0 ) {
            linksCache.put(key, links);
            return links;
        }

        for ( Relationship relationship : relationships.values() ) {
            if ( relationship.getRelationshipType() == REFERENCE ) {
                if ( schema.getResourceFields().containsKey(relationship.getPropertyName()) ) {
                    links.add(relationship.getName());
                }
            } else if ( relationship.getRelationshipType() == CHILD ) {
                Schema other = schemaFactory.getSchema(relationship.getObjectType());
                if ( other != null )
                    links.add(relationship.getName());
            }
        }

        linksCache.put(key, links);
        return links;
    }

    @Override
    public Relationship getRelationship(String type, String linkName) {
        Class<?> clz = schemaFactory.getSchemaClass(type);
        Map<String,Relationship> relationship = relationships.get(clz);
        return relationship == null ? null : relationship.get(linkName);
    }

    @PostConstruct
    public void init() {
        InitializationUtils.onInitialization(this, typeSets);
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public List<TypeSet> getTypeSets() {
        return typeSets;
    }

    @Inject
    public void setTypeSets(List<TypeSet> typeSets) {
        this.typeSets = typeSets;
    }

}
