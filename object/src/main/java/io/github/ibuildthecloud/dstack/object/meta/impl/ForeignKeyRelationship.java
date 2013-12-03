package io.github.ibuildthecloud.dstack.object.meta.impl;

import io.github.ibuildthecloud.dstack.object.meta.Relationship;

import org.jooq.ForeignKey;

public class ForeignKeyRelationship implements Relationship {

    ForeignKey<?, ?> foreignKey;
    String propertyName;
    Class<?> objectType;
    RelationshipType relationshipType;
    String name;

    public ForeignKeyRelationship(RelationshipType relationshipType, String name, String propertyName,
            Class<?> objectType, ForeignKey<?, ?> foreignKey) {
        super();
        this.name = name;
        this.relationshipType = relationshipType;
        this.propertyName = propertyName;
        this.objectType = objectType;
        this.foreignKey = foreignKey;
    }

    public ForeignKey<?, ?> getForeignKey() {
        return foreignKey;
    }

    @Override
    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Class<?> getObjectType() {
        return objectType;
    }

    @Override
    public String getName() {
        return name;
    }

}
