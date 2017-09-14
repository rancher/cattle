package io.github.ibuildthecloud.gdapi.validation;

import io.github.ibuildthecloud.gdapi.annotation.Field;

public class SubType {

    String testField, notWrite;

    @Field(create = true)
    public String getTestField() {
        return testField;
    }

    public void setTestField(String testField) {
        this.testField = testField;
    }

    @Field(create = false)
    public String getNotWrite() {
        return notWrite;
    }

    public void setNotWrite(String notWrite) {
        this.notWrite = notWrite;
    }

}
