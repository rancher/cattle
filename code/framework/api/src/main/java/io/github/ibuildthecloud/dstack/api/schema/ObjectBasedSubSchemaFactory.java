package io.github.ibuildthecloud.dstack.api.schema;

import io.github.ibuildthecloud.dstack.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.util.type.Named;
import io.github.ibuildthecloud.gdapi.factory.impl.SubSchemaFactory;
import io.github.ibuildthecloud.model.impl.SchemaImpl;

import java.util.Map;

import javax.inject.Inject;

public class ObjectBasedSubSchemaFactory extends SubSchemaFactory implements Named {

    ObjectMetaDataManager metaDataManager;

    @Override
    protected void prune(SchemaImpl schema) {
        super.prune(schema);

        schema.getIncludeableLinks().clear();

        Map<String,Relationship> rels = metaDataManager.getLinkRelationships(this, schema.getId());
        if ( rels == null || rels.size() == 0 ) {
            return;
        } else {
            schema.getIncludeableLinks().addAll(rels.keySet());
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
