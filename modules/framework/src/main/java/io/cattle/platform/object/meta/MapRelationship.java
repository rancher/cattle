package io.cattle.platform.object.meta;

public interface MapRelationship extends Relationship {

    Class<?> getMappingType();

    Relationship getSelfRelationship();

    Relationship getOtherRelationship();

}
