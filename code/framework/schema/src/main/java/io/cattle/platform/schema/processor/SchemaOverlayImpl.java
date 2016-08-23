package io.cattle.platform.schema.processor;

import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

public class SchemaOverlayImpl extends SchemaImpl {

    private static final long serialVersionUID = -7324841846559047697L;

    boolean resourceFieldsExplicit;
    boolean resourceActionsExplicit;
    boolean collectionActionsExplicit;
    boolean collectionFieldsExplicit;
    boolean collectionFiltersExplicit;

    public boolean isResourceFieldsExplicit() {
        return resourceFieldsExplicit;
    }

    public void setResourceFieldsExplicit(boolean resourceFieldsExplicit) {
        this.resourceFieldsExplicit = resourceFieldsExplicit;
    }

    public boolean isResourceActionsExplicit() {
        return resourceActionsExplicit;
    }

    public void setResourceActionsExplicit(boolean resourceActionsExplicit) {
        this.resourceActionsExplicit = resourceActionsExplicit;
    }

    public boolean isCollectionActionsExplicit() {
        return collectionActionsExplicit;
    }

    public void setCollectionActionsExplicit(boolean collectionActionsExplicit) {
        this.collectionActionsExplicit = collectionActionsExplicit;
    }

    public boolean isCollectionFieldsExplicit() {
        return collectionFieldsExplicit;
    }

    public void setCollectionFieldsExplicit(boolean collectionFieldsExplicit) {
        this.collectionFieldsExplicit = collectionFieldsExplicit;
    }

    public boolean isCollectionFiltersExplicit() {
        return collectionFiltersExplicit;
    }

    public void setCollectionFiltersExplicit(boolean collectionFiltersExplicit) {
        this.collectionFiltersExplicit = collectionFiltersExplicit;
    }

}
