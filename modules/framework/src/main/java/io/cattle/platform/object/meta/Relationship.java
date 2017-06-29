package io.cattle.platform.object.meta;

public interface Relationship {

    enum RelationshipType {
        CHILD, REFERENCE, MAP
    }

    boolean isListResult();

    RelationshipType getRelationshipType();

    String getName();

    String getPropertyName();

    Class<?> getObjectType();

}
