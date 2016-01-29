package io.github.ibuildthecloud.gdapi.model.impl;

import io.github.ibuildthecloud.gdapi.model.Field;
import io.github.ibuildthecloud.gdapi.model.FieldType;
import io.github.ibuildthecloud.gdapi.model.FieldType.TypeAndName;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

public class FieldImpl implements Field {

    String name, type, validChars, invalidChars, transform, description;
    Integer displayIndex;
    boolean create, update, includeInList = true, nullable, unique, required, defaultIsNull, readOnCreateOnly = false;
    FieldType typeEnum;
    List<FieldType> subTypeEnums;
    List<String> subTypes;
    Long min, max, minLength, maxLength;
    Object defaultValue;
    List<String> options;
    Method readMethod;
    Map<String, Object> attributes = new HashMap<String, Object>();

    public FieldImpl() {
    }

    public FieldImpl(Field field) {
        this.name = field.getName();
        this.description = field.getDescription();
        this.validChars = field.getValidChars();
        this.invalidChars = field.getInvalidChars();
        this.create = field.isCreate();
        this.readOnCreateOnly = field.isReadOnCreateOnly();
        this.transform = field.getTransform();
        this.update = field.isUpdate();
        this.includeInList = field.isIncludeInList();
        this.nullable = field.isNullable();
        this.unique = field.isUnique();
        this.required = field.isRequired();
        this.min = field.getMin();
        this.max = field.getMax();
        this.minLength = field.getMinLength();
        this.maxLength = field.getMaxLength();
        this.defaultValue = field.getDefault();
        this.options = field.getOptions() == null ? null : new ArrayList<String>(field.getOptions());
        this.displayIndex = field.getDisplayIndex();
        this.attributes = new HashMap<String, Object>(field.getAttributes());
        if (field instanceof FieldImpl) {
            this.readMethod = ((FieldImpl)field).getReadMethod();
            this.defaultIsNull = ((FieldImpl)field).isDefaultIsNull();
        }

        setType(field.getType());
    }

    @Override
    public Object getValue(Object object) {
        if (readMethod == null || object == null)
            return null;

        try {
            return readMethod.invoke(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @XmlTransient
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    @Override
    public boolean isReadOnCreateOnly() {
        return readOnCreateOnly;
    }

    public void setReadOnCreateOnly(boolean readOnCreateOnly) {
        this.readOnCreateOnly = readOnCreateOnly;
    }

    @Override
    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    @Override
    public boolean isIncludeInList() {
        return includeInList;
    }

    public void setIncludeInList(boolean includeInList) {
        this.includeInList = includeInList;
    }

    @Override
    public String getType() {
        if (type == null && typeEnum != null) {
            type = FieldType.toString(type, typeEnum, subTypes);
        }
        return type;
    }

    public void setType(String typeName) {
        if (typeName == null) {
            type = null;
        } else {
            List<TypeAndName> parts = FieldType.parse(typeName);
            if (parts.size() == 0) {
                throw new IllegalArgumentException("Failed to parse type [" + typeName + "]");
            }

            TypeAndName part = parts.get(0);
            this.type = part.getName();
            this.typeEnum = part.getType();

            parts.remove(0);
            setSubTypesList(parts);
        }
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public FieldType getTypeEnum() {
        return typeEnum;
    }

    public void setTypeEnum(FieldType typeEnum) {
        this.typeEnum = typeEnum;
        this.type = null;
        this.subTypeEnums = Collections.emptyList();
        this.subTypes = Collections.emptyList();
    }

    @Override
    public Long getMin() {
        return min;
    }

    public void setMin(Long min) {
        this.min = min;
    }

    @Override
    public Long getMax() {
        return max;
    }

    public void setMax(Long max) {
        this.max = max;
    }

    @Override
    public Long getMinLength() {
        return minLength;
    }

    public void setMinLength(Long minLength) {
        this.minLength = minLength;
    }

    @Override
    public Long getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Long maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public Object getDefault() {
        if (defaultValue == null || this.typeEnum == null) {
            return defaultValue;
        }

        switch (this.typeEnum) {
        case BOOLEAN:
            return Boolean.parseBoolean(defaultValue.toString());
        case INT:
            try {
                return Long.parseLong(defaultValue.toString());
            } catch (NumberFormatException nfe) {
                break;
            }
        default:
        }

        return defaultValue;
    }

    public void setDefault(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @XmlTransient
    public Method getReadMethod() {
        return readMethod;
    }

    public void setReadMethod(Method readMethod) {
        this.readMethod = readMethod;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    @Override
    public String getValidChars() {
        return validChars;
    }

    public void setValidChars(String validChars) {
        this.validChars = validChars;
    }

    @Override
    public String getInvalidChars() {
        return invalidChars;
    }

    public void setInvalidChars(String invalidChars) {
        this.invalidChars = invalidChars;
    }

    @Override
    public String toString() {
        return "FieldImpl [name=" + name + ", type=" + type + ", validChars=" + validChars + ", invalidChars=" + invalidChars + ", displayIndex="
                + displayIndex + ", create=" + create + ", update=" + update + ", includeInList=" + includeInList + ", nullable=" + nullable + ", unique="
                + unique + ", required=" + required + ", defaultIsNull=" + defaultIsNull + ", typeEnum=" + typeEnum + ", subTypeEnums=" + subTypeEnums
                + ", subTypes=" + subTypes + ", min=" + min + ", max=" + max + ", minLength=" + minLength + ", maxLength=" + maxLength + ", defaultValue="
                + defaultValue + ", options=" + options + ", readMethod=" + readMethod + "description=" + description + "]";
    }

    public void setSubTypesList(List<TypeAndName> subTypes) {
        this.subTypes = new ArrayList<String>(subTypes.size());
        this.subTypeEnums = new ArrayList<FieldType>(subTypes.size());

        for (TypeAndName typeAndName : subTypes) {
            this.subTypes.add(typeAndName.getName());
            this.subTypeEnums.add(typeAndName.getType());
        }

        this.type = FieldType.toString(type, typeEnum, this.subTypes);
    }

    @Override
    public List<FieldType> getSubTypeEnums() {
        return subTypeEnums;
    }

    @Override
    public List<String> getSubTypes() {
        return subTypes;
    }

    @Override
    @XmlTransient
    public Integer getDisplayIndex() {
        return displayIndex;
    }

    public void setDisplayIndex(Integer displayIndex) {
        this.displayIndex = displayIndex;
    }

    @XmlTransient
    public boolean isDefaultIsNull() {
        return defaultIsNull;
    }

    public void setDefaultIsNull(boolean defaultIsNull) {
        this.defaultIsNull = defaultIsNull;
    }

    @Override
    public boolean hasDefault() {
        return defaultValue != null || defaultIsNull;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @XmlTransient
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public String getTransform() {
        return transform;
    }

}
