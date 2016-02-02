package io.github.ibuildthecloud.gdapi.factory.impl;

import io.github.ibuildthecloud.gdapi.annotation.Actions;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Action;
import io.github.ibuildthecloud.gdapi.model.ApiError;
import io.github.ibuildthecloud.gdapi.model.ApiVersion;
import io.github.ibuildthecloud.gdapi.model.Collection;
import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.FieldType.TypeAndName;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.FieldImpl;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

@io.github.ibuildthecloud.gdapi.annotation.Type
public class SchemaFactoryImpl extends AbstractSchemaFactory implements SchemaFactory {

    final io.github.ibuildthecloud.gdapi.annotation.Field defaultField;
    final io.github.ibuildthecloud.gdapi.annotation.Type defaultType;

    String id = UUID.randomUUID().toString();
    boolean includeDefaultTypes = true, writableByDefault = false;
    Map<String, SchemaImpl> schemasByName = new TreeMap<String, SchemaImpl>();
    Map<Class<?>, SchemaImpl> schemasByClass = new HashMap<Class<?>, SchemaImpl>();
    Map<String, Class<?>> typeToClass = new HashMap<String, Class<?>>();
    List<Class<?>> types = new ArrayList<Class<?>>();
    List<String> typeNames = new ArrayList<String>();
    Map<SchemaImpl, Class<?>> parentClasses = new HashMap<SchemaImpl, Class<?>>();

    List<Schema> schemasList = new ArrayList<Schema>();
    List<SchemaPostProcessor> postProcessors = new ArrayList<SchemaPostProcessor>();

    public SchemaFactoryImpl() {
        try {
            defaultField =
                    PropertyUtils.getPropertyDescriptor(this, "defaultField").getReadMethod()
                            .getAnnotation(io.github.ibuildthecloud.gdapi.annotation.Field.class);

            defaultType = this.getClass().getAnnotation(io.github.ibuildthecloud.gdapi.annotation.Type.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @io.github.ibuildthecloud.gdapi.annotation.Field
    public Object getDefaultField() {
        return null;
    }

    @Override
    public Schema registerSchema(Object obj) {
        Class<?> clz = obj instanceof Class<?> ? (Class<?>)obj : null;
        SchemaImpl schema = schemaFromObject(obj);

        for (SchemaPostProcessor processor : postProcessors) {
            schema = processor.postProcessRegister(schema, this);
            if (schema == null) {
                return null;
            }
        }

        /* Register in the multitude of maps */
        if (clz != null) {
            addToMap(typeToClass, schema, clz);
        }
        addToMap(schemasByName, schema, schema);

        if (clz != null) {
            schemasByClass.put(clz, schema);
            for (Class<?> iface : clz.getInterfaces()) {
                schemasByClass.put(iface, schema);
            }
        }

        schemasList.add(schema);

        return schema;
    }

    protected <T> void addToMap(Map<String, T> map, SchemaImpl key, T value) {
        if (key == null || value == null)
            return;

        map.put(key.getId(), value);
        map.put(key.getId().toLowerCase(), value);

        map.put(key.getPluralName(), value);
        map.put(key.getPluralName().toLowerCase(), value);
    }

    @Override
    public Schema getSchema(Class<?> clz) {
        return schemasByClass.get(clz);
    }

    @Override
    public Schema parseSchema(String name) {
        SchemaImpl schema = readSchema(name);
        Class<?> clz = typeToClass.get(name);

        processParent(schema);

        List<Field> fields = getFields(clz);
        for (Map.Entry<String, Field> entry : schema.getResourceFields().entrySet()) {
            Field field = entry.getValue();
            if (field instanceof FieldImpl) {
                ((FieldImpl)field).setName(entry.getKey());
            }
            fields.add(field);
        }

        Map<String, Field> resourceFields = sortFields(fields);

        schema.setResourceFields(resourceFields);
        schema.getResourceActions().putAll(getResourceActions(clz));
        schema.getCollectionActions().putAll(getCollectionActions(clz));

        for (SchemaPostProcessor processor : postProcessors) {
            schema = processor.postProcess(schema, this);
        }

        addToMap(schemasByName, schema, schema);

        if (clz == null && schema.getParent() != null) {
            clz = typeToClass.get(schema.getParent());
            if (clz != null) {
                addToMap(typeToClass, schema, clz);
            }
        }

        return schema;
    }

    protected void processParent(SchemaImpl schema) {
        SchemaImpl parent = null;
        Class<?> parentClass = parentClasses.get(schema);
        String parentName = schema.getParent();

        if (parentClass == null && parentName != null) {
            parent = schemasByName.get(parentName);
            if (parent == null) {
                throw new IllegalArgumentException("Failed to find parent schema for [" + parentName + "] for type [" + schema.getId() + "]");
            }
        } else if (parentClass != null) {
            parent = schemasByClass.get(parentClass);
            if (parent == null) {
                throw new IllegalArgumentException("Failed to find parent schema for class [" + parentClass + "] for type [" + schema.getId() + "]");
            }
        }

        if (parent != null) {
            schema.setParent(parent.getId());
            parent.getChildren().add(schema.getId());

            schema.load(parent);
        }
    }

    protected Map<String, Action> getCollectionActions(Class<?> clz) {
        return getActions(clz, true);
    }

    protected Map<String, Action> getResourceActions(Class<?> clz) {
        return getActions(clz, false);
    }

    protected Map<String, Action> getActions(Class<?> clz, boolean collection) {
        Map<String, Action> result = new LinkedHashMap<String, Action>();

        if (clz == null) {
            return result;
        }

        Actions actions = clz.getAnnotation(Actions.class);
        if (actions == null) {
            return result;
        }

        for (io.github.ibuildthecloud.gdapi.annotation.Action action : actions.value()) {
            if (action.collection() != collection) {
                continue;
            }

            String input = null;
            String output = null;

            if (StringUtils.isBlank(action.inputType())) {
                input = getSchemaName(action.input());
            } else {
                input = action.inputType();
            }

            if (StringUtils.isBlank(action.outputType())) {
                output = getSchemaName(action.output());
            } else {
                output = action.outputType();
            }

            result.put(action.name(), new Action(input, output));
        }

        return result;
    }

    protected SchemaImpl schemaFromObject(Object obj) {
        Class<?> clz = obj instanceof Class<?> ? (Class<?>)obj : obj.getClass();
        SchemaImpl schema = new SchemaImpl();

        SchemaType schemaType = obj instanceof String ? schemaTypeFromString((String)obj) : schemaTypeFromClass(clz);

        schema.setName(schemaType.name);
        schema.setPluralName(schemaType.pluralName);
        schema.setParent(schemaType.parent);

        if (schemaType.parent == null && schemaType.parentClass != null) {
            parentClasses.put(schema, schemaType.parentClass);
        }

        return schema;
    }

    protected SchemaType schemaTypeFromClass(Class<?> clz) {
        SchemaType schemaType = new SchemaType();
        io.github.ibuildthecloud.gdapi.annotation.Type type = clz.getAnnotation(io.github.ibuildthecloud.gdapi.annotation.Type.class);

        if (type == null)
            type = defaultType;

        if (!StringUtils.isEmpty(type.name())) {
            schemaType.name = type.name();
        } else {
            schemaType.name = StringUtils.uncapitalize(clz.getSimpleName());
        }

        if (!StringUtils.isBlank(type.pluralName())) {
            schemaType.pluralName = type.pluralName();
        }

        if (!StringUtils.isBlank(type.parent())) {
            schemaType.parent = type.parent();
        }

        if (type.parentClass() != Void.class) {
            schemaType.parentClass = type.parentClass();
        }

        return schemaType;
    }

    protected SchemaType schemaTypeFromString(String type) {
        SchemaType schemaType = new SchemaType();

        String[] parts = type.split("\\s*,\\s*");
        schemaType.name = parts[0];

        for (int i = 1; i < parts.length; i++) {
            String[] kv = parts[i].split("\\s*=\\s*");
            if (kv.length != 2) {
                throw new IllegalArgumentException("Illegal type format [" + type + "] must be comma separated key=value pairs");
            }

            String key = kv[0];
            String value = kv[1];

            if ("pluralName".equals(key)) {
                schemaType.pluralName = value;
            } else if ("parent".equals(key)) {
                schemaType.parent = value;
            }
        }

        return schemaType;
    }

    protected SchemaImpl readSchema(String name) {
        Class<?> clz = typeToClass.get(name);
        if (clz == null)
            clz = Object.class;

        SchemaImpl schema = schemasByName.get(name);
        if (schema == null)
            schema = schemaFromObject(clz);

        io.github.ibuildthecloud.gdapi.annotation.Type type = clz.getAnnotation(io.github.ibuildthecloud.gdapi.annotation.Type.class);

        if (type == null)
            type = defaultType;

        if (type == defaultType) {
            schema.setCreate(writableByDefault);
            schema.setUpdate(writableByDefault);
            schema.setDeletable(writableByDefault);
        } else {
            schema.setCreate(type.create());
            schema.setUpdate(type.update());
            schema.setDeletable(type.delete());
        }
        schema.setById(type.byId());
        schema.setList(type.list());

        return schema;
    }

    protected Map<String, Field> sortFields(List<Field> fields) {
        Map<Integer, Field> indexed = new TreeMap<Integer, Field>();
        Map<String, Field> named = new TreeMap<String, Field>();
        Map<String, Field> result = new LinkedHashMap<String, Field>();

        for (Field field : fields) {
            Integer displayIndex = field.getDisplayIndex();

            if (displayIndex == null) {
                named.put(field.getName(), field);
            } else {
                indexed.put(displayIndex, field);
            }
        }

        for (Field field : indexed.values()) {
            result.put(field.getName(), field);
        }

        for (Field field : named.values()) {
            result.put(field.getName(), field);
        }

        return result;
    }

    protected List<Field> getFields(Class<?> clz) {
        List<Field> result = new ArrayList<Field>();

        if (clz == null)
            return result;

        for (PropertyDescriptor prop : PropertyUtils.getPropertyDescriptors(clz)) {
            FieldImpl field = getField(clz, prop);
            if (field != null) {
                result.add(field);
            }
        }

        return result;
    }

    protected FieldImpl getField(Class<?> clz, PropertyDescriptor prop) {
        FieldImpl field = new FieldImpl();

        Method readMethod = prop.getReadMethod();
        Method writeMethod = prop.getWriteMethod();
        if (readMethod == null && writeMethod == null)
            return null;

        io.github.ibuildthecloud.gdapi.annotation.Field f = getFieldAnnotation(prop);

        if (!f.include())
            return null;

        field.setReadMethod(readMethod);

        if (readMethod != null && readMethod.getDeclaringClass() != clz)
            return null;

        if (StringUtils.isEmpty(f.name())) {
            field.setName(prop.getName());
        } else {
            field.setName(f.name());
        }

        if (StringUtils.isNotEmpty(f.description())) {
            field.setDescription(f.description());
        }

        if (f.displayIndex() > 0) {
            field.setDisplayIndex(f.displayIndex());
        }

        if (readMethod == null) {
            field.setIncludeInList(false);
        }

        assignSimpleProps(field, f);
        assignType(prop, field, f);
        assignLengths(field, f);
        assignOptions(prop, field, f);

        return field;
    }

    protected void assignOptions(PropertyDescriptor prop, FieldImpl field, io.github.ibuildthecloud.gdapi.annotation.Field f) {
        Class<?> clz = prop.getPropertyType();

        if (!clz.isEnum()) {
            return;
        }

        List<String> options = new ArrayList<String>(clz.getEnumConstants().length);
        for (Object o : clz.getEnumConstants()) {
            options.add(o.toString());
        }

        field.setOptions(options);
    }

    protected void assignSimpleProps(FieldImpl field, io.github.ibuildthecloud.gdapi.annotation.Field f) {
        if (!StringUtils.isEmpty(f.defaultValue())) {
            field.setDefault(f.defaultValue());
        }

        if (!StringUtils.isEmpty(f.validChars())) {
            field.setValidChars(f.validChars());
        }

        if (!StringUtils.isEmpty(f.invalidChars())) {
            field.setInvalidChars(f.invalidChars());
        }

        if (f == this.defaultField) {
            field.setNullable(writableByDefault);
            field.setUpdate(writableByDefault);
            field.setCreate(writableByDefault);
        } else {
            field.setNullable(f.nullable());
            field.setUpdate(f.update());
            field.setCreate(f.create());
            field.setTransform(f.transform());
        }
        field.setUnique(f.unique());
        field.setRequired(f.required());
    }

    protected void assignLengths(FieldImpl field, io.github.ibuildthecloud.gdapi.annotation.Field f) {
        if (f.min() != Long.MIN_VALUE) {
            field.setMin(f.min());
        }

        if (f.max() != Long.MAX_VALUE) {
            field.setMax(f.max());
        }

        if (f.minLength() != Long.MIN_VALUE) {
            field.setMinLength(f.minLength());
        }

        if (f.maxLength() != Long.MAX_VALUE) {
            field.setMaxLength(f.maxLength());
        }
    }

    protected void assignType(PropertyDescriptor prop, FieldImpl field, io.github.ibuildthecloud.gdapi.annotation.Field f) {
        if (f.type() != FieldType.NONE) {
            field.setTypeEnum(f.type());
            return;
        }

        if (!StringUtils.isEmpty(f.typeString())) {
            field.setType(f.typeString());
            return;
        }

        if (f.password()) {
            field.setTypeEnum(FieldType.PASSWORD);
            return;
        }

        assignSimpleType(prop.getPropertyType(), field);

        List<TypeAndName> types = new ArrayList<FieldType.TypeAndName>();
        Method readMethod = prop.getReadMethod();
        if (readMethod != null) {
            getTypes(readMethod.getGenericReturnType(), types);
        }

        if (types.size() == 1) {
            field.setType(types.get(0).getName());
        } else if (types.size() > 1) {
            types.remove(0);
            field.setSubTypesList(types);
        }
    }

    protected void getTypes(java.lang.reflect.Type type, List<TypeAndName> types) {
        Class<?> clz = null;
        if (type instanceof Class<?>) {
            clz = (Class<?>)type;
        }

        if (type instanceof ParameterizedType) {
            java.lang.reflect.Type rawType = ((ParameterizedType)type).getRawType();
            if (rawType instanceof Class<?>)
                clz = (Class<?>)rawType;
        }

        if (clz == null) {
            throw new IllegalArgumentException("Failed to find class for type [" + type + "]");
        }

        FieldType fieldType = assignSimpleType(clz, null);
        String name = fieldType.getExternalType();
        if (fieldType == FieldType.TYPE) {
            Schema subSchema = getSchema(clz);
            if (subSchema == null) {
                fieldType = FieldType.JSON;
                name = fieldType.getExternalType();
            } else {
                name = subSchema.getId();
            }
        }

        types.add(new TypeAndName(fieldType, name));

        java.lang.reflect.Type subType = null;
        switch (fieldType) {
        case ARRAY:
            if (clz.isArray()) {
                subType = clz.getComponentType();
            } else {
                subType = getGenericType(type, 0);
            }
            break;
        case MAP:
            subType = getGenericType(type, 1);
            break;
        case REFERENCE:
            subType = getGenericType(type, 0);
            break;
        case TYPE:
            return;
        default:
            break;
        }

        if (subType != null) {
            getTypes(subType, types);
        }
    }

    protected java.lang.reflect.Type getGenericType(java.lang.reflect.Type t, int index) {
        if (t instanceof ParameterizedType && ((ParameterizedType)t).getActualTypeArguments().length == index + 1) {
            return ((ParameterizedType)t).getActualTypeArguments()[index];
        }

        return Object.class;
    }

    protected FieldType assignSimpleType(Class<?> clzType, FieldImpl field) {
        FieldType result = null;

        if (clzType.isEnum()) {
            result = FieldType.ENUM;
        } else {
            outer: for (FieldType type : FieldType.values()) {
                Class<?>[] clzs = type.getClasses();

                if (clzs == null)
                    continue;

                for (Class<?> clz : clzs) {
                    if (clz.isAssignableFrom(clzType)) {
                        result = type;

                        if ((Number.class.isAssignableFrom(clzType) || Boolean.class.isAssignableFrom(clzType)) && !clz.isPrimitive() && field != null) {
                            field.setNullable(true);
                        }

                        break outer;
                    }
                }
            }
        }

        if (field != null) {
            field.setTypeEnum(result);
        }

        return result;
    }

    protected io.github.ibuildthecloud.gdapi.annotation.Field getFieldAnnotation(PropertyDescriptor prop) {
        Method readMethod = prop.getReadMethod();
        Method writeMethod = prop.getWriteMethod();

        io.github.ibuildthecloud.gdapi.annotation.Field f = null;

        if (readMethod != null) {
            f = readMethod.getAnnotation(io.github.ibuildthecloud.gdapi.annotation.Field.class);
        }

        if (f == null && writeMethod != null) {
            f = writeMethod.getAnnotation(io.github.ibuildthecloud.gdapi.annotation.Field.class);
        }

        if (f == null) {
            f = defaultField;
        }

        return f;
    }

    @PostConstruct
    public void init() {
        if (includeDefaultTypes) {
            registerSchema(Schema.class);
            registerSchema(ApiVersion.class);
            registerSchema(ApiError.class);
            registerSchema(Collection.class);
            registerSchema(Resource.class);
        }

        for (Class<?> clz : types) {
            registerSchema(clz);
        }

        for (String name : typeNames) {
            registerSchema(name);
        }

        for (Schema schema : schemasList) {
            parseSchema(schema.getId());
        }
    }

    @Override
    public List<Schema> listSchemas() {
        return schemasList;
    }

    @Override
    public Schema getSchema(String type) {
        return schemasByName.get(lower(type));
    }

    @Override
    public Class<?> getSchemaClass(String type) {
        return typeToClass.get(lower(type));
    }

    protected String lower(String type) {
        return type == null ? "" : type.toLowerCase();
    }

    public List<Class<?>> getTypes() {
        return types;
    }

    public void setTypes(List<Class<?>> types) {
        this.types = types;
    }

    public List<SchemaPostProcessor> getPostProcessors() {
        return postProcessors;
    }

    public void setPostProcessors(List<SchemaPostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }

    public List<String> getTypeNames() {
        return typeNames;
    }

    public void setTypeNames(List<String> typeNames) {
        this.typeNames = typeNames;
    }

    public boolean isIncludeDefaultTypes() {
        return includeDefaultTypes;
    }

    public void setIncludeDefaultTypes(boolean includeDefaultTypes) {
        this.includeDefaultTypes = includeDefaultTypes;
    }

    public boolean isWritableByDefault() {
        return writableByDefault;
    }

    public void setWritableByDefault(boolean writableByDefault) {
        this.writableByDefault = writableByDefault;
    }

    public void setId(String id) {
        this.id = id;
    }

    private static final class SchemaType {
        String name;
        String pluralName;
        String parent;
        Class<?> parentClass;
    }

}
