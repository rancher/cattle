package io.cattle.platform.object.meta.impl;

import io.cattle.platform.object.meta.MapRelationship;
import io.cattle.platform.object.meta.Relationship;

public class MapRelationshipImpl implements MapRelationship {

    String name;
    Class<?> mappingType;
    Class<?> objectType;
    Relationship selfRelationship;
    Relationship otherRelationship;

    public MapRelationshipImpl(String name, Class<?> mappingType, Class<?> objectType, Relationship selfRelationship, Relationship otherRelationship) {
        super();
        this.name = name;
        this.mappingType = mappingType;
        this.objectType = objectType;
        this.selfRelationship = selfRelationship;
        this.otherRelationship = otherRelationship;
    }

    @Override
    public boolean isListResult() {
        return true;
    }

    @Override
    public RelationshipType getRelationshipType() {
        return RelationshipType.MAP;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPropertyName() {
        return selfRelationship.getPropertyName();
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    @Override
    public Class<?> getMappingType() {
        return mappingType;
    }

    @Override
    public Relationship getSelfRelationship() {
        return selfRelationship;
    }

    @Override
    public Relationship getOtherRelationship() {
        return otherRelationship;
    }

}