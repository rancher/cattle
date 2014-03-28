package io.cattle.platform.configitem.model;

public interface ItemVersion {

    long getRevision();

    String getSourceRevision();

    boolean isLatest();

    String toExternalForm();

}
