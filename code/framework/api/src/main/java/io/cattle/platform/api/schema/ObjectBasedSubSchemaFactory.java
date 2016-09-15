package io.cattle.platform.api.schema;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.util.type.Named;
import io.github.ibuildthecloud.gdapi.factory.impl.SubSchemaFactory;
import io.github.ibuildthecloud.gdapi.model.impl.SchemaImpl;

import java.util.Map;

import javax.inject.Inject;

public class ObjectBasedSubSchemaFactory extends SubSchemaFactory implements Named {

    ObjectMetaDataManager metaDataManager;

    @Override
    protected void prune(SchemaImpl schema) {
        super.prune(schema);

        schema.getIncludeableLinks().clear();

        Map<String, Relationship> rels = metaDataManager.getLinkRelationships(this, schema.getId());
        if (rels == null || rels.size() == 0) {
            return;
        } else {
            for (Relationship rel : rels.values()) {
                schema.getIncludeableLinks().add(rel.getName());
            }
        }
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

    @Override
    public String getName() {
        return "schema:" + getId();
    }

}
