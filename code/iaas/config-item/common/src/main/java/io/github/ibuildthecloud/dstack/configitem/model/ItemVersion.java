package io.github.ibuildthecloud.dstack.configitem.model;

public interface ItemVersion {

    long getRevision();

    String getSourceRevision();

    boolean isLatest();

}
