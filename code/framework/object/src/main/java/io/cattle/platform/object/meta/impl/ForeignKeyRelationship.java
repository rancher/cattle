package io.cattle.platform.object.meta.impl;

import io.cattle.platform.object.meta.Relationship;

import org.jooq.ForeignKey;

public class ForeignKeyRelationship implements Relationship {

    ForeignKey<?, ?> foreignKey;
    String propertyName;
    Class<?> objectType;
    RelationshipType relationshipType;
    String name;

    public ForeignKeyRelationship(RelationshipType relationshipType, String name, String propertyName, Class<?> objectType, ForeignKey<?, ?> foreignKey) {
        super();
        this.name = name;
        this.relationshipType = relationshipType;
        this.propertyName = propertyName;
        this.objectType = objectType;
        this.foreignKey = foreignKey;
    }

    @Override
    public boolean isListResult() {
        return relationshipType == RelationshipType.CHILD;
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
