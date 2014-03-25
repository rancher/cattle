package io.github.ibuildthecloud.dstack.object.meta;

public interface MapRelationship extends Relationship {

    Class<?> getMappingType();

    Relationship getSelfRelationship();

    Relationship getOtherRelationship();

}
