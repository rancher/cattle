package io.github.ibuildthecloud.gdapi.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

public interface Field {

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    String getName();

    String getType();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    FieldType getTypeEnum();

    Object getDefault();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    boolean hasDefault();

    boolean isUnique();

    boolean isNullable();

    boolean isCreate();

    boolean isReadOnCreateOnly();

    boolean isRequired();

    boolean isUpdate();

    Long getMinLength();

    Long getMaxLength();

    Long getMin();

    Long getMax();

    List<String> getOptions();

    String getValidChars();

    String getInvalidChars();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    boolean isIncludeInList();

    Object getValue(Object object);

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    List<FieldType> getSubTypeEnums();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    List<String> getSubTypes();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    Integer getDisplayIndex();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    Map<String, Object> getAttributes();

}
