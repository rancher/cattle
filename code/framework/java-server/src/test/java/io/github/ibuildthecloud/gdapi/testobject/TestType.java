package io.github.ibuildthecloud.gdapi.testobject;

import io.github.ibuildthecloud.gdapi.annotation.Field;
import io.github.ibuildthecloud.gdapi.model.IdRef;

import java.io.FileInputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestType {

    private enum TestEnum {
        FIRST, SECOND
    }

    String gonnaBeNameOverride;
    String defaultSettings;
    String first, second, a;
    String name;
    String onlyWriteable;
    String defaultValue;
    String nullable;
    FileInputStream typeBlob;
    Date typeDate;
    TestEnum typeEnum;
    boolean typeBool;
    Boolean typeBoolean;
    float typeFloat;
    Float typeFloatObject;
    int typeInt;
    Integer typeInteger;
    long typeLong;
    Long typeLongObject;
    double typeDouble;
    Double typeDoubleObject;
    String typePassword;
    String typeString;
    String[] typeArray;
    List<Map<String, String>> typeList;
    Map<String, String> typeMap;
    List<TestTypeCRUD> typeListCrud;
    IdRef<TestType> typeReference;
    Long lengths;
    String createUpdate;
    String unique;
    String required;
    String validChars;
    String invalidChars;
    IdRef<TestTypeCRUD> testTypeCrudId;
    TestTypeCRUD testTypeCrud;
    Map<String, Object> typeMapObject;
    List<TestEnum> testEnumList;

    public List<TestEnum> getTestEnumList() {
        return testEnumList;
    }

    public void setTestEnumList(List<TestEnum> testEnumList) {
        this.testEnumList = testEnumList;
    }

    public void setOnlyWriteable(String onlyWriteable) {
        this.onlyWriteable = onlyWriteable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Field(displayIndex = 1)
    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    @Field(displayIndex = 2)
    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public FileInputStream getTypeBlob() {
        return typeBlob;
    }

    public void setTypeBlob(FileInputStream typeBlob) {
        this.typeBlob = typeBlob;
    }

    public Date getTypeDate() {
        return typeDate;
    }

    public void setTypeDate(Date typeDate) {
        this.typeDate = typeDate;
    }

    public TestEnum getTypeEnum() {
        return typeEnum;
    }

    public void setTypeEnum(TestEnum typeEnum) {
        this.typeEnum = typeEnum;
    }

    public boolean isTypeBool() {
        return typeBool;
    }

    public void setTypeBool(boolean typeBool) {
        this.typeBool = typeBool;
    }

    public Boolean getTypeBoolean() {
        return typeBoolean;
    }

    public void setTypeBoolean(Boolean typeBoolean) {
        this.typeBoolean = typeBoolean;
    }

    public float getTypeFloat() {
        return typeFloat;
    }

    public void setTypeFloat(float typeFloat) {
        this.typeFloat = typeFloat;
    }

    public Float getTypeFloatObject() {
        return typeFloatObject;
    }

    public void setTypeFloatObject(Float typeFloatObject) {
        this.typeFloatObject = typeFloatObject;
    }

    public int getTypeInt() {
        return typeInt;
    }

    public void setTypeInt(int typeInt) {
        this.typeInt = typeInt;
    }

    public Integer getTypeInteger() {
        return typeInteger;
    }

    public void setTypeInteger(Integer typeInteger) {
        this.typeInteger = typeInteger;
    }

    public long getTypeLong() {
        return typeLong;
    }

    public void setTypeLong(long typeLong) {
        this.typeLong = typeLong;
    }

    public Long getTypeLongObject() {
        return typeLongObject;
    }

    public void setTypeLongObject(Long typeLongObject) {
        this.typeLongObject = typeLongObject;
    }

    public double getTypeDouble() {
        return typeDouble;
    }

    public void setTypeDouble(double typeDouble) {
        this.typeDouble = typeDouble;
    }

    public Double getTypeDoubleObject() {
        return typeDoubleObject;
    }

    public void setTypeDoubleObject(Double typeDoubleObject) {
        this.typeDoubleObject = typeDoubleObject;
    }

    @Field(password = true)
    public String getTypePassword() {
        return typePassword;
    }

    public void setTypePassword(String typePassword) {
        this.typePassword = typePassword;
    }

    public String getTypeString() {
        return typeString;
    }

    public void setTypeString(String typeString) {
        this.typeString = typeString;
    }

    public String[] getTypeArray() {
        return typeArray;
    }

    public void setTypeArray(String[] typeArray) {
        this.typeArray = typeArray;
    }

    public Map<String, String> getTypeMap() {
        return typeMap;
    }

    public void setTypeMap(Map<String, String> typeMap) {
        this.typeMap = typeMap;
    }

    public IdRef<TestType> getTypeReference() {
        return typeReference;
    }

    public void setTypeReference(IdRef<TestType> typeReference) {
        this.typeReference = typeReference;
    }

    public String getDefaultSettings() {
        return defaultSettings;
    }

    public void setDefaultSettings(String defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

    @Field(min = 342, max = 442, minLength = 142, maxLength = 242)
    public Long getLengths() {
        return lengths;
    }

    public void setLengths(Long lengths) {
        this.lengths = lengths;
    }

    @Field(defaultValue = "DEFAULT")
    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Field(name = "nameOverride")
    public String getGonnaBeNameOverride() {
        return gonnaBeNameOverride;
    }

    public void setGonnaBeNameOverride(String gonnaBeNameOverride) {
        this.gonnaBeNameOverride = gonnaBeNameOverride;
    }

    @Field(create = true, update = true)
    public String getCreateUpdate() {
        return createUpdate;
    }

    public void setCreateUpdate(String createUpdate) {
        this.createUpdate = createUpdate;
    }

    @Field(nullable = true)
    public String getNullable() {
        return nullable;
    }

    public void setNullable(String nullable) {
        this.nullable = nullable;
    }

    @Field(unique = true)
    public String getUnique() {
        return unique;
    }

    public void setUnique(String unique) {
        this.unique = unique;
    }

    @Field(required = true)
    public String getRequired() {
        return required;
    }

    public void setRequired(String required) {
        this.required = required;
    }

    @Field(validChars = "valid")
    public String getValidChars() {
        return validChars;
    }

    public void setValidChars(String validChars) {
        this.validChars = validChars;
    }

    @Field(invalidChars = "invalid")
    public String getInvalidChars() {
        return invalidChars;
    }

    public void setInvalidChars(String invalidChars) {
        this.invalidChars = invalidChars;
    }

    public IdRef<TestTypeCRUD> getTestTypeCrudId() {
        return testTypeCrudId;
    }

    public void setTestTypeCrudId(IdRef<TestTypeCRUD> testTypeCrudId) {
        this.testTypeCrudId = testTypeCrudId;
    }

    public TestTypeCRUD getTestTypeCrud() {
        return testTypeCrud;
    }

    public void setTestTypeCrud(TestTypeCRUD testTypeCrud) {
        this.testTypeCrud = testTypeCrud;
    }

    public List<Map<String, String>> getTypeList() {
        return typeList;
    }

    public void setTypeList(List<Map<String, String>> typeList) {
        this.typeList = typeList;
    }

    public List<TestTypeCRUD> getTypeListCrud() {
        return typeListCrud;
    }

    public void setTypeListCrud(List<TestTypeCRUD> typeListCrud) {
        this.typeListCrud = typeListCrud;
    }

    public Map<String, Object> getTypeMapObject() {
        return typeMapObject;
    }

    public void setTypeMapObject(Map<String, Object> typeMapObject) {
        this.typeMapObject = typeMapObject;
    }

}
