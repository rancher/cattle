package io.cattle.platform.object.meta.impl;

import com.google.common.collect.Lists;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.engine.process.StateTransition;
import io.cattle.platform.object.jooq.utils.JooqUtils;
import io.cattle.platform.object.meta.ActionDefinition;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.meta.TypeSet;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.condition.ConditionType;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaFactoryImpl;
import io.github.ibuildthecloud.gdapi.factory.impl.SchemaPostProcessor;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.Filter;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;
import org.apache.commons.beanutils.PropertyUtils;
import org.jooq.ForeignKey;
import org.jooq.Table;
import org.jooq.TableField;

import javax.persistence.Column;
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
import java.util.TreeSet;
import java.util.WeakHashMap;

import static io.cattle.platform.object.meta.Relationship.RelationshipType.*;

public class DefaultObjectMetaDataManager implements ObjectMetaDataManager {

    SchemaFactory schemaFactory;
    List<TypeSet> typeSets;
    Map<Class<?>, Map<String, List<Relationship>>> relationships = new HashMap<>();
    Map<Class<?>, Map<String, List<Relationship>>> relationshipsBothCase = new HashMap<>();
    List<ProcessDefinition> processDefinitions = new ArrayList<>();

    Map<String, Set<String>> validStates = new HashMap<>();
    Map<String, Set<String>> transitioningStates = new HashMap<>();
    Map<String, Set<String>> actions = new HashMap<>();
    Map<String, Map<String, String>> linksCache = Collections.synchronizedMap(new WeakHashMap<String, Map<String, String>>());
    Map<FieldCacheKey, String> propertyCache = Collections.synchronizedMap(new WeakHashMap<FieldCacheKey, String>());
    Map<String, Map<String, ActionDefinition>> actionDefinitions = new HashMap<>();
    Map<FieldCacheKey, TableField<?, ?>> tableFields = new HashMap<>();

    public DefaultObjectMetaDataManager(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
        init();
    }

    private void init() {
        if (schemaFactory instanceof SchemaFactoryImpl) {
            ((SchemaFactoryImpl) schemaFactory).getPostProcessors().add(0, new SchemaPostProcessor() {
                @Override
                public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory) {
                    return DefaultObjectMetaDataManager.this.postProcessRegister(schema, factory);
                }

                @Override
                public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
                    return DefaultObjectMetaDataManager.this.postProcess(schema, factory);
                }
            });
        }
    }

    public void start() {
        List<Schema> schemas = registerTypes();
        registerActions();
        registerActionDefinitions();
        registerStates();
        registerRelationships();
        parseSchemas(schemas);
    }

    protected void registerActions() {
        actions.clear();

        for (ProcessDefinition def : processDefinitions) {
            String resourceType = def.getResourceType();
            Set<String> actions = this.actions.get(resourceType);

            if (resourceType == null) {
                continue;
            }

            if (def.getName().startsWith(resourceType.toLowerCase() + ".")) {
                if (actions == null) {
                    actions = new LinkedHashSet<>();
                    this.actions.put(resourceType, actions);
                }

                actions.add(def.getName().substring(resourceType.length() + 1));
            }
        }
    }

    protected void registerStates() {
        transitioningStates.clear();

        for (ProcessDefinition def : processDefinitions) {
            Set<String> validStates = this.validStates.get(def.getResourceType());
            Set<String> states = transitioningStates.get(def.getResourceType());

            for (StateTransition transition : def.getStateTransitions()) {
                if (states == null) {
                    states = new HashSet<>();
                    transitioningStates.put(def.getResourceType(), states);
                }

                if (validStates == null) {
                    validStates = new TreeSet<>();
                    this.validStates.put(def.getResourceType(), validStates);
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

                if (ObjectMetaDataManager.STATE_FIELD.equals(transition.getField())) {
                    validStates.add(transition.getToState());
                    validStates.add(transition.getFromState());
                }
            }
        }
    }

    protected void registerActionDefinitions() {
        actionDefinitions.clear();

        for (ProcessDefinition processDef : processDefinitions) {
            String type = processDef.getResourceType();
            String processName = processDef.getName();

            if (processName.startsWith(type.toLowerCase())) {
                processName = processName.substring(type.length() + 1, processName.length());
            }

            Map<String, ActionDefinition> actionDefs = actionDefinitions.get(type);

            if (actionDefs == null) {
                actionDefs = new HashMap<>();
                actionDefinitions.put(type, actionDefs);
            }

            ActionDefinition def = actionDefs.get(processName);
            if (def == null) {
                def = new ActionDefinition();
                actionDefs.put(processName, def);
            }

            for (StateTransition transition : processDef.getStateTransitions()) {
                if (transition.getType() != StateTransition.Style.DONE) {
                    def.getValidStates().add(transition.getFromState());
                }
            }
        }
    }

    protected void registerRelationships() {
        for (TypeSet typeSet : typeSets) {
            for (Class<?> clz : typeSet.getTypeClasses()) {
                Table<?> table = JooqUtils.getTableFromRecordClass(clz);
                if (table == null) {
                    continue;
                }

                registerTableFields(table);
                for (ForeignKey<?, ?> reference : table.getReferences()) {
                    register(reference);
                }
            }
        }
    }

    protected void registerTableFields(Table<?> table) {
        Class<?> clz = table.getClass();
        for (java.lang.reflect.Field field : clz.getFields()) {
            if (TableField.class.isAssignableFrom(field.getType()) && Modifier.isPublic(field.getModifiers())) {
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
        List<Schema> schemas = new ArrayList<>();

        for (TypeSet typeSet : typeSets) {
            for (Class<?> type : typeSet.getTypeClasses()) {
                Schema schema = schemaFactory.registerSchema(type);
                if (schema != null)
                    schemas.add(schema);
            }
            for (String name : typeSet.getTypeNames()) {
                Schema schema = schemaFactory.registerSchema(name);
                if (schema != null)
                    schemas.add(schema);
            }
        }

        return schemas;
    }

    protected void parseSchemas(List<Schema> schemas) {
        Set<String> done = new HashSet<>();
        List<Schema> todo = new ArrayList<>(schemas);

        do {
            int todoSize = todo.size();

            Iterator<Schema> iter = todo.iterator();
            while (iter.hasNext()) {
                Schema schema = iter.next();
                if (schema.getParent() == null || done.contains(schema.getParent())) {
                    schemaFactory.parseSchema(schema.getId());
                    done.add(schema.getId());
                    iter.remove();
                }
            }

            if (todo.size() > 0 && todo.size() == todoSize) {
                throw new IllegalStateException("Failed to find parents of schemas " + todo);
            }
        } while (todo.size() > 0);
    }

    protected void register(ForeignKey<?, ?> foreignKey) {
        TableField<?, ?>[] fields = foreignKey.getFieldsArray();
        if (fields.length == 0 || fields.length > 1) {
            return;
        }
        TableField<?, ?> field = fields[0];

        if (!field.getDataType().isNumeric()) {
            return;
        }

        String propertyName = getNameFromField(field.getTable().getRecordType(), field.getName());
        String referenceName = propertyName;
        if (field.getName().endsWith(ID_FIELD)) {
            referenceName = referenceName.substring(0, referenceName.length() - 2);
        }

        Class<?> localType = foreignKey.getTable().getRecordType();
        Class<?> foreignType = foreignKey.getKey().getTable().getRecordType();

        Schema localSchema = schemaFactory.getSchema(localType);
        String childName = localSchema.getPluralName();

        String childNameOverride = ArchaiusUtil.getString(
                String.format("object.link.name.%s.%s.override", localType.getSimpleName(), propertyName).toLowerCase()).get();

        if (childNameOverride != null) {
            childName = childNameOverride;
        }

        register(localType, new ForeignKeyRelationship(REFERENCE, referenceName, propertyName, foreignType, foreignKey));
        register(foreignType, new ForeignKeyRelationship(CHILD, childName, propertyName, localType, foreignKey));
    }

    protected void register(Class<?> type, Relationship relationship) {
        register(this.relationships, type, relationship, false);
        register(this.relationshipsBothCase, type, relationship, true);
    }

    protected void register(Map<Class<?>, Map<String, List<Relationship>>> relationshipsMap, Class<?> type, Relationship relationship, boolean bothCase) {
        Map<String, List<Relationship>> relationships = relationshipsMap.get(type);
        if (relationships == null) {
            relationships = new HashMap<>();
            relationshipsMap.put(type, relationships);
        }

        String name = relationship.getName().toLowerCase();
        List<Relationship> existing = relationships.get(name);

        if (existing != null) {
            existing.add(relationship);
            if (bothCase) {
                relationships.get(relationship.getName()).add(relationship);
            }
            return;
        }

        relationships.put(relationship.getName().toLowerCase(), Lists.newArrayList(relationship));
        if (bothCase) {
            relationships.put(relationship.getName(), Lists.newArrayList(relationship));
        }
    }

    @Override
    public Object convertFieldNameFor(String type, Object key) {
        return getTableFieldFor(type, key);
    }

    protected TableField<?, ?> getTableFieldFor(String type, Object key) {
        if (key instanceof TableField<?, ?>)
            return (TableField<?, ?>) key;

        Class<?> clz = schemaFactory.getSchemaClass(type);
        FieldCacheKey cacheKey = new FieldCacheKey(clz, key.toString());

        return tableFields.get(cacheKey);
    }

    @Override
    public String convertToPropertyNameString(Class<?> recordClass, Object key) {
        if (key instanceof String) {
            return (String) key;
        }

        if (key instanceof TableField) {
            TableField<?, ?> field = (TableField<?, ?>) key;
            return getNameFromField(recordClass, field.getName());
        }

        return key == null ? null : key.toString();
    }

    protected String getNameFromField(Class<?> clz, String field) {
        FieldCacheKey key = new FieldCacheKey(clz, field);
        String cached = propertyCache.get(key);

        if (cached != null)
            return cached;

        for (PropertyDescriptor desc : PropertyUtils.getPropertyDescriptors(clz)) {
            Method readMethod = desc.getReadMethod();
            Method writeMethod = desc.getWriteMethod();

            if (readMethod == null || writeMethod == null) {
                continue;
            }

            Column column = readMethod.getAnnotation(Column.class);
            if (column != null && field.equals(column.name())) {
                propertyCache.put(key, desc.getName());
                return desc.getName();
            }
        }

        throw new IllegalArgumentException("Failed to find bean property for table field [" + field + "] on [" + clz + "]");
    }

    public SchemaImpl postProcessRegister(SchemaImpl schema, SchemaFactory factory) {
        return schema;
    }

    protected void addStates(SchemaImpl schema, SchemaFactory factory) {
        String type = factory.getBaseType(schema.getId());
        Set<String> validStates = this.validStates.get(type);
        Field stateField = schema.getResourceFields().get(ObjectMetaDataManager.STATE_FIELD);

        if (validStates != null && stateField instanceof FieldImpl) {
            FieldImpl field = (FieldImpl) stateField;
            field.setOptions(new ArrayList<>(validStates));
            field.setTypeEnum(FieldType.ENUM);
        }
    }

    protected void addActions(SchemaImpl schema, SchemaFactory factory) {
        Set<String> actions = this.actions.get(schema.getId());
        if (actions == null || actions.size() == 0) {
            return;
        }

        Map<String, Action> resourceActions = schema.getResourceActions();
        if (resourceActions == null) {
            resourceActions = new LinkedHashMap<>();
            schema.setResourceActions(resourceActions);
        }

        for (String action : actions) {
            if (!resourceActions.containsKey(action)) {
                Action newAction = new Action();
                newAction.setOutput(schema.getId());
                resourceActions.put(action, newAction);
            }
        }
    }

    protected void addTransitioningFields(SchemaImpl schema, SchemaFactory factory) {
        Set<String> states = transitioningStates.get(schema.getId());
        if (states == null || states.size() == 0) {
            return;
        }

        addField(schema, TRANSITIONING_FIELD, FieldType.ENUM, TRANSITIONING_YES, TRANSITIONING_NO, TRANSITIONING_ERROR);
        addField(schema, TRANSITIONING_MESSAGE_FIELD, FieldType.STRING);
    }

    protected void addField(SchemaImpl schema, String name, FieldType type, String... options) {
        Field f = schema.getResourceFields().get(name);
        if (f != null) {
            return;
        }

        FieldImpl newField = new FieldImpl();
        newField.setTypeEnum(type);
        newField.setName(name);
        if (type == FieldType.ENUM) {
            newField.setOptions(Arrays.asList(options));
        } else {
            newField.setNullable(true);
        }

        schema.getResourceFields().put(name, newField);
    }

    public SchemaImpl postProcess(SchemaImpl schema, SchemaFactory factory) {
        addStates(schema, factory);
        addActions(schema, factory);
        addTransitioningFields(schema, factory);

        Map<String, List<Relationship>> relationships = this.relationships.get(factory.getSchemaClass(schema.getId()));

        if (relationships != null) {
            List<Relationship> allRelationships = new ArrayList<>();
            for (List<Relationship> relationshipList : relationships.values()) {
                allRelationships.addAll(relationshipList);
            }
            for (Relationship relationship : allRelationships) {
                if (relationship.getRelationshipType() != REFERENCE) {
                    continue;
                }

                Field field = schema.getResourceFields().get(relationship.getPropertyName());
                if (!(field instanceof FieldImpl)) {
                    continue;
                }

                FieldImpl fieldImpl = (FieldImpl) field;
                fieldImpl.setType(FieldType.toString(FieldType.REFERENCE, factory.getSchema(relationship.getObjectType()).getId()));
            }
        }

        Map<String, Filter> filters = schema.getCollectionFilters();

        for (Map.Entry<String, Field> entry : schema.getResourceFields().entrySet()) {
            String name = entry.getKey();
            Field field = entry.getValue();
            if (!(field instanceof FieldImpl)) {
                continue;
            }

            FieldImpl fieldImpl = (FieldImpl) field;
            TableField<?, ?> tableField = getTableFieldFor(schema.getId(), name);

            if (tableField != null && !filters.containsKey(name)) {
                List<String> modifiers = getModifiers(fieldImpl);
                if (modifiers.size() > 0) {
                    filters.put(name, new Filter(modifiers));
                }
            }
        }

        return schema;
    }

    protected List<String> getModifiers(FieldImpl field) {
        FieldType type = field.getTypeEnum();
        if (type == null) {
            return Collections.emptyList();
        }

        List<String> conditions = new ArrayList<>(type.getModifiers().size() + 2);
        for (ConditionType conditionType : type.getModifiers()) {
            conditions.add(conditionType.getExternalForm());
        }

        if (field.isNullable()) {
            if (!conditions.contains(ConditionType.NULL.getExternalForm())) {
                conditions.add(ConditionType.NULL.getExternalForm());
            }
            if (!conditions.contains(ConditionType.NOTNULL.getExternalForm())) {
                conditions.add(ConditionType.NOTNULL.getExternalForm());
            }
        }

        return conditions;
    }

    @Override
    public Map<String, Relationship> getLinkRelationships(SchemaFactory schemaFactory, String type) {
        if (schemaFactory == null) {
            schemaFactory = this.schemaFactory;
        }
        Map<String, Relationship> result = new HashMap<>();
        Schema schema = schemaFactory.getSchema(type);

        if (null == schema) {
            return null;
        }

        Map<String, List<Relationship>> relationships = this.relationships.get(schemaFactory.getSchemaClass(schema.getId()));
        if (relationships == null) {
            return result;
        }

        for (String link : getLinks(schemaFactory, type).keySet()) {
            link = link.toLowerCase();
            List<Relationship> relList = relationships.get(link);
            if (relList != null) {
                if (relList.size() > 1) {
                    for (Relationship rel : relList) {
                        if (rel.getName().toLowerCase().equals(link)) {
                            result.put(link, rel);
                        }
                    }
                } else {
                    result.put(link, relList.get(0));
                }
            }
        }

        return result;
    }

    @Override
    public Map<String, String> getLinks(SchemaFactory schemaFactory, String type) {
        if (schemaFactory == null) {
            schemaFactory = this.schemaFactory;
        }
        String key = schemaFactory.getId() + ":links:" + type;
        Map<String, String> links = linksCache.get(key);
        if (links != null)
            return links;

        links = new TreeMap<>();
        Schema schema = schemaFactory.getSchema(type);

        Map<String, List<Relationship>> relationships = this.relationships.get(schemaFactory.getSchemaClass(schema.getId(), true));
        if (relationships == null || relationships.size() == 0) {
            linksCache.put(key, links);
            return links;
        }
        List<Relationship> allRelationships = new ArrayList<>();
        for (List<Relationship> relationshipList : relationships.values()) {
            allRelationships.addAll(relationshipList);
        }
        for (Relationship relationship : allRelationships) {
            if (relationship.isListResult()) {
                Schema other = schemaFactory.getSchema(relationship.getObjectType());
                if (other != null)
                    links.put(relationship.getName(), null);
            } else {
                if (schema.getResourceFields().containsKey(relationship.getPropertyName())) {
                    links.put(relationship.getName(), relationship.getPropertyName());
                }
            }
        }

        linksCache.put(key, links);
        return links;
    }

    @Override
    public Relationship getRelationship(String type, String linkName) {
        Class<?> clz = schemaFactory.getSchemaClass(type, true);
        if (clz == null) {
            return null;
        }
        Map<String, List<Relationship>> relationship = relationshipsBothCase.get(clz);
        List<Relationship> relationshipList = relationship.get(linkName);
        return getRelationship(relationshipList, linkName);
    }

    @Override
    public Relationship getRelationship(Class<?> clz, String linkName) {
        if (clz == null) {
            return null;
        }
        Map<String, List<Relationship>> relationship = relationshipsBothCase.get(clz);
        List<Relationship> relationshipList = relationship.get(linkName);
        return getRelationship(relationshipList, linkName);
    }


    private Relationship getRelationship(List<Relationship> relationshipList, String linkName) {
        if (relationshipList == null || relationshipList.size() == 0) {
            return null;
        }
        return relationshipList.get(0);
    }

    @Override
    public Map<String, Object> getTransitionFields(Schema schema, Object obj) {
        Set<String> states = transitioningStates.get(schema.getId());
        if (states == null || states.size() == 0) {
            schema = schemaFactory.getSchema(obj.getClass());
            states = transitioningStates.get(schema.getId());

            if (states == null) {
                return Collections.emptyMap();
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put(TRANSITIONING_FIELD, TRANSITIONING_NO);
        result.put(TRANSITIONING_MESSAGE_FIELD, null);

        String message = DataAccessor.fieldString(obj, TRANSITIONING_MESSAGE_FIELD);

        String state = DataAccessor.getState(obj);
        if (TRANSITIONING_ERROR_OVERRIDE.equals(DataAccessor.fieldString(obj, TRANSITIONING_FIELD))) {
            Map<String, Object> errorResult = new LinkedHashMap<>();
            errorResult.put(TRANSITIONING_FIELD, TRANSITIONING_ERROR);
            return errorResult;
        } else if (state != null && states.contains(state)) {
            result.put(TRANSITIONING_FIELD, TRANSITIONING_YES);
            result.put(TRANSITIONING_MESSAGE_FIELD, message == null ? TRANSITIONING_MESSAGE_DEFAULT_FIELD : message);
        } else if (TRANSITIONING_ERROR.equals(DataAccessor.fieldString(obj, TRANSITIONING_FIELD))) {
            return Collections.emptyMap();
        }

        return result;
    }

    @Override
    public boolean isTransitioningState(Class<?> resourceType, String state) {
        Schema schema = schemaFactory.getSchema(resourceType);
        Set<String> states = transitioningStates.get(schema.getId());

        if (states == null) {
            return false;
        }

        return state != null && states.contains(state);
    }

    @Override
    public Map<String, ActionDefinition> getActionDefinitions(Object obj) {
        if (obj == null) {
            return null;
        }

        Schema schema = schemaFactory.getSchema(obj.getClass());
        if (schema == null) {
            return null;
        }

        String type = schemaFactory.getBaseType(schema.getId());

        return actionDefinitions.get(type);
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    public List<TypeSet> getTypeSets() {
        return typeSets;
    }

    public void setTypeSets(List<TypeSet> typeSets) {
        this.typeSets = typeSets;
    }

    public List<ProcessDefinition> getProcessDefinitions() {
        return processDefinitions;
    }

    public void setProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

}
