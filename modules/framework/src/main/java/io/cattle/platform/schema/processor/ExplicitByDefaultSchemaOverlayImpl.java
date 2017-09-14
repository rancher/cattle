package io.cattle.platform.schema.processor;

public class ExplicitByDefaultSchemaOverlayImpl extends SchemaOverlayImpl {

    private static final long serialVersionUID = 218740296276625396L;

    public ExplicitByDefaultSchemaOverlayImpl() {
        setCollectionActionsExplicit(true);
        setCollectionFieldsExplicit(true);
        setCollectionFiltersExplicit(true);
        setResourceActionsExplicit(true);
        setResourceFieldsExplicit(true);
    }

}
