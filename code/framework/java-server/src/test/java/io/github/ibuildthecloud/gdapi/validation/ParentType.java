package io.github.ibuildthecloud.gdapi.validation;

import io.github.ibuildthecloud.gdapi.annotation.Field;

import java.util.List;

public class ParentType {

    SubType subType;
    List<SubType> subTypes;

    @Field(create = true)
    public List<SubType> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(List<SubType> subTypes) {
        this.subTypes = subTypes;
    }

    @Field(create = true)
    public SubType getSubType() {
        return subType;
    }

    public void setSubType(SubType subType) {
        this.subType = subType;
    }

}
