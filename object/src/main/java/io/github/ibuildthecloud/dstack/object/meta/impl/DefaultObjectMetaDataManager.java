package io.github.ibuildthecloud.dstack.object.meta.impl;

import static io.github.ibuildthecloud.dstack.object.meta.Relationship.RelationshipType.*;
import io.github.ibuildthecloud.dstack.engine.process.ProcessDefinition;
import io.github.ibuildthecloud.dstack.engine.process.StateTransition;
import io.github.ibuildthecloud.dstack.object.jooq.utils.JooqUtils;
import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.meta.TypeSet;
import io.github.ibuildthecloud.dstack.object.util.DataUtils;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Filter;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.model.impl.FieldImpl;
import io.github.ibuildthecloud.model.impl.SchemaImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;

import javax.inject.Inject;
import javax.persistence.Column;

import org.apache.commons.beanutils.PropertyUtils;
import org.jooq.ForeignKey;
import org.jooq.Table;
import org.jooq.TableField;

public class DefaultObjectMetaDataManager implements ObjectMetaDataManager, SchemaPostProcessor, InitializationTask {

    SchemaFactory schemaFactory;
    List<TypeSet> typeSets;
    Map<Class<?>,Map<String,Relationship>> relationships = new HashMap<Class<?>, Map<String,Relationship>>();
    Map<Class<?>,Map<String,Relationship>> relationshipsBothCase = new HashMap<Class<?>, Map<String,Relationship>>();
    List<ProcessDefinition> processDefinitions;

    Map<String,Set<String>> transitioningStates = new HashMap<String, Set<String>>();
    Map<String,Set<String>> actions = new HashMap<String, Set<String>>();
    Map<String,Map<String,String>> linksCache = Collections.synchronizedMap(new WeakHashMap<String,Map<String,String>>());
    Map<FieldCacheKey, String> propertyCache = Collections.synchronizedMap(new WeakHashMap<FieldCacheKey, String>());
    Map<FieldCacheKey, TableField<?, ?>> tableFields = new HashMap<FieldCacheKey, TableField<?,?>>();

    @Override
    public void start() {
        List<Schema> schemas = registerTypes();
        registerActions();
        registerTransitionStates();
        registerRelationships();
        parseSchemas(schemas);
    }

    @Override
    public void stop() {
    }

    protected void registerActions() {
        actions.clear();

        for ( ProcessDefinition def : processDefinitions ) {
            String resourceType = def.getResourceType();
            Set<String> actions = this.actions.get(resourceType);

            if ( resourceType == null ) {
                continue;
            }

            if ( def.getName().startsWith(resourceType.toLowerCase() + ".") ) {
                if ( actions == null ) {
                    actions = new LinkedHashSet<String>();
                    this.actions.put(resourceType, actions);
                }

                actions.add(def.getName().substring(resourceType.length() + 1));
            }
        }
    }

    protected void registerTransitionStates() {
        transitioningStates.clear();

        for ( ProcessDefinition def : processDefinitions ) {
            Set<String> states = transitioningStates.get(def.getResourceType());

            for ( StateTransition transition : def.getStateTransitions() ) {
                if ( states == null ) {
                    states = new HashSet<String>();
                    transitioningStates.put(def.getResourceType(), states);
                }

                switch (transition.getType()) {
                case DONE:
                    states.add(transition.getFromState());
                    break;
                case TRANSITIONING:
                    states.add(transition.getToState());
                    break;
                default:
                }
            }
        }
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
                Schema schema = schemaFactory.registerSchema(name);
                if ( schema != null )
                    schemas.add(schema);
            }
            for ( Class<?> type : typeSet.getTypeClasses() ) {
                Schema schema = schemaFactory.registerSchema(type);
                if ( schema != null )
                    schemas.add(schema);
            }
        }

        return schemas;
    }

    protected void parseSchemas(List<Schema> schemas) {
        Set<String> done = new HashSet<String>();
        List<Schema> todo = new ArrayList<Schema>(schemas);

        do {
            int todoSize = todo.size();

            Iterator<Schema> iter = todo.iterator();
            while ( iter.hasNext() ) {
                Schema schema = iter.next();
                if ( schema.getParent() == null || done.contains(schema.getParent()) ) {
                    schemaFactory.parseSchema(schema.getId());
                    done.add(schema.getId());
                    iter.remove();
                }
            }

            if ( todo.size() > 0 && todo.size() == todoSize ) {
                throw new IllegalStateException("Failed to find parents of schemas " + todo);
            }
        } while ( todo.size() > 0 );
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
        register(this.relationships, type, relationship, false);
        register(this.relationshipsBothCase, type, relationship, true);
    }

    protected void register(Map<Class<?>,Map<String,Relationship>> relationshipsMap, Class<?> type, Relationship relationship,
            boolean bothCase) {
        Map<String,Relationship> relationships = relationshipsMap.get(type);
        if ( relationships == null ) {
            relationships = new HashMap<String, Relationship>();
            relationshipsMap.put(type, relationships);
        }

        relationships.put(relationship.getName().toLowerCase(), relationship);
        if ( bothCase ) {
            relationships.put(relationship.getName(), relationship);
        }
    }


    @Override
    public Object convertFieldNameFor(String type, Object key) {
        return getTableFieldFor(type, key);
    }

    protected TableField<?,?> getTableFieldFor(String type, Object key) {
        if ( key instanceof TableField<?, ?> )
            return (TableField<?,?>)key;

        Class<?> clz = schemaFactory.getSchemaClass(type);
        FieldCacheKey cacheKey = new FieldCacheKey(clz, key.toString());

        return tableFields.get(cacheKey);
    }

    @Override
    public String convertToPropertyNameString(Class<?> recordClass, Object key) {
        if ( key instanceof String ) {
            return (String)key;
        }

        if ( key instanceof TableField ) {
            TableField<?, ?> field = (TableField<?, ?>)key;
            return getNameFromField(recordClass, field.getName());
        }

        return key == null ? null : key.toString();
    }

    @Override
    public String lookupPropertyNameFromFieldName(Class<?> recordClass, String fieldName) {
        return getNameFromField(recordClass, fieldName);
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
    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory) {
        return schema;
    }

    protected void addActions(SchemaImpl schema, SchemaFactory factory) {
        Set<String> actions = this.actions.get(schema.getId());
        if ( actions == null || actions.size() == 0 ) {
            return;
        }

        Map<String,Action> resourceActions = schema.getResourceActions();
        if ( resourceActions == null ) {
            resourceActions = new LinkedHashMap<String, Action>();
            schema.setResourceActions(resourceActions);
        }

        for ( String action : actions ) {
            if ( ! resourceActions.containsKey(action) ) {
                Action newAction = new Action();
                newAction.setOutput(schema.getId());
                resourceActions.put(action, newAction);
            }
        }
    }
    protected void addTransitioningFields(SchemaImpl schema, SchemaFactory factory) {
        Set<String> states = transitioningStates.get(schema.getId());
        if ( states == null || states.size() == 0 ) {
            return;
        }

        addField(schema, TRANSITIONING_FIELD, FieldType.ENUM, TRANSITIONING_YES, TRANSITIONING_NO, TRANSITIONING_ERROR);
        addField(schema, TRANSITIONING_MESSAGE_FIELD, FieldType.STRING);
        addField(schema, TRANSITIONING_PROGRESS_FIELD, FieldType.INT);
    }

    protected void addField(SchemaImpl schema, String name, FieldType type, String... options) {
        Field f = schema.getResourceFields().get(name);
        if ( f != null ) {
            return;
        }

        FieldImpl newField = new FieldImpl();
        newField.setTypeEnum(type);
        newField.setName(name);
        if ( type == FieldType.ENUM ) {
            newField.setOptions(Arrays.asList(options));
        } else {
            newField.setNullable(true);
        }

        schema.getResourceFields().put(name, newField);
    }

    @Override
    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
        addActions(schema, factory);
        addTransitioningFields(schema, factory);

        Map<String,Relationship> relationships = this.relationships.get(factory.getSchemaClass(schema.getId()));

        if ( relationships != null ) {
            for ( Map.Entry<String,Relationship> entry : relationships.entrySet() ) {
                String linkName = entry.getKey();
                Relationship relationship = entry.getValue();

                if ( relationship.getRelationshipType() != REFERENCE ) {
                    schema.getIncludeableLinks().add(linkName);
                    continue;
                }

                Field field = schema.getResourceFields().get(relationship.getPropertyName());
                if ( ! ( field instanceof FieldImpl ) ) {
                    continue;
                }

                FieldImpl fieldImpl = (FieldImpl)field;
                fieldImpl.setType(FieldType.toString(FieldType.REFERENCE, factory.getSchema(relationship.getObjectType()).getId()));

                schema.getIncludeableLinks().add(linkName);
            }
        }

        Map<String,Filter> filters = schema.getCollectionFilters();

        for ( Map.Entry<String,Field> entry : schema.getResourceFields().entrySet() ) {
            String name = entry.getKey();
            Field field = entry.getValue();
            if ( ! ( field instanceof FieldImpl ) ) {
                continue;
            }

            FieldImpl fieldImpl = (FieldImpl)field;
            TableField<?, ?> tableField = getTableFieldFor(schema.getId(), name);

            if ( tableField != null && ! filters.containsKey(name) ) {
                List<String> modifiers = getModifiers(fieldImpl);
                if ( modifiers.size() > 0 ) {
                    filters.put(name, new Filter(modifiers));
                }
            }
        }

        return schema;
    }

    protected List<String> getModifiers(FieldImpl field) {
        FieldType type = field.getTypeEnum();
        if ( type == null ) {
            return Collections.emptyList();
        }

        List<String> conditions = new ArrayList<String>(type.getModifiers().size() + 2);
        for ( ConditionType conditionType : type.getModifiers() ) {
            conditions.add(conditionType.getExternalForm());
        }

        if ( field.isNullable() ) {
            conditions.add(ConditionType.NULL.getExternalForm());
            conditions.add(ConditionType.NOTNULL.getExternalForm());
        }

        return conditions;
    }

    @Override
    public Map<String,Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type) {
        if ( schemaFactory == null ) {
            schemaFactory = this.schemaFactory;
        }
        Map<String,Relationship> result = new HashMap<String, Relationship>();
        Schema schema = schemaFactory.getSchema(type);

        Map<String,Relationship> relationships = this.relationships.get(schemaFactory.getSchemaClass(schema.getId()));
        for ( String link : getLinks(schemaFactory, type).keySet() ) {
            Relationship rel = relationships.get(link);
            if ( rel != null ) {
                result.put(link, rel);
            }
        }

        return result;
    }

    @Override
    public Map<String,String> getLinks(SchemaFactory schemaFactory, String type) {
        if ( schemaFactory == null ) {
            schemaFactory = this.schemaFactory;
        }
        String key = schemaFactory.getId() + ":links:" + type;
        Map<String,String> links = linksCache.get(key);
        if ( links != null )
            return links;

        links = new TreeMap<String,String>();
        Schema schema = schemaFactory.getSchema(type);

        Map<String,Relationship> relationships = this.relationships.get(schemaFactory.getSchemaClass(schema.getId(), true));
        if ( relationships == null || relationships.size() == 0 ) {
            linksCache.put(key, links);
            return links;
        }

        for ( Relationship relationship : relationships.values() ) {
            if ( relationship.getRelationshipType() == REFERENCE ) {
                if ( schema.getResourceFields().containsKey(relationship.getPropertyName()) ) {
                    links.put(relationship.getName(), relationship.getPropertyName());
                }
            } else if ( relationship.getRelationshipType() == CHILD ) {
                Schema other = schemaFactory.getSchema(relationship.getObjectType());
                if ( other != null )
                    links.put(relationship.getName(), null);
            }
        }

        linksCache.put(key, links);
        return links;
    }

    @Override
    public Relationship getRelationship(String type, String linkName) {
        Class<?> clz = schemaFactory.getSchemaClass(type, true);
        Map<String,Relationship> relationship = relationshipsBothCase.get(clz);
        return relationship == null ? null : relationship.get(linkName);
    }

    @Override
    public Map<String, Object> getTransitionFields(Schema schema, Object obj) {
        Set<String> states = transitioningStates.get(schema.getId());
        if ( states == null || states.size() == 0 ) {
            schema = schemaFactory.getSchema(obj.getClass());
            states = transitioningStates.get(schema.getId());

            if ( states == null ) {
                return Collections.emptyMap();
            }
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put(TRANSITIONING_FIELD, TRANSITIONING_NO);

        String state = DataUtils.getState(obj);
        if ( state != null && states.contains(state) ) {
            result.put(TRANSITIONING_FIELD, TRANSITIONING_YES);
            result.put(TRANSITIONING_MESSAGE_FIELD, TRANSITIONING_MESSAGE_DEFAULT_FIELD);
            result.put(TRANSITIONING_PROGRESS_FIELD, null);
        }

        return result;
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

    public List<ProcessDefinition> getProcessDefinitions() {
        return processDefinitions;
    }

    @Inject
    public void setProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

}