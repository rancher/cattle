package io.github.ibuildthecloud.gdapi.model;

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonInclude;

public interface Field {

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    String getName();

    String getDescription();

    String getType();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    FieldType getTypeEnum();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    Object getDefault();

    @XmlTransient
    @io.github.ibuildthecloud.gdapi.annotation.Field(include = false)
    boolean hasDefault();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean isUnique();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean isNullable();

    boolean isCreate();

    boolean isReadOnCreateOnly();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    boolean isRequired();

    boolean isUpdate();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    Long getMinLength();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    Long getMaxLength();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    Long getMin();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    Long getMax();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    List<String> getOptions();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    String getValidChars();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    String getInvalidChars();

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    String getTransform();

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
