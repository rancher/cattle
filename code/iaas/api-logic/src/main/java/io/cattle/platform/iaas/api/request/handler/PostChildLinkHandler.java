package io.cattle.platform.iaas.api.request.handler;

import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.meta.Relationship;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.handler.AbstractApiRequestHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Inject;

public class PostChildLinkHandler extends AbstractApiRequestHandler {

    ObjectMetaDataManager metaDataManager;

    @Override
    public void handle(ApiRequest request) throws IOException {
        if (!Method.POST.isMethod(request.getMethod()) || request.getLink() == null) {
            return;
        }

        Map<String, Relationship> rels = metaDataManager.getLinkRelationships(request.getSchemaFactory(), request.getType());
        Relationship rel = rels == null ? null : rels.get(request.getLink());

        if (rel == null || rel.getRelationshipType() != Relationship.RelationshipType.CHILD) {
            return;
        }

        Schema childSchema = request.getSchemaFactory().getSchema(rel.getObjectType());
        if (childSchema == null) {
            return;
        }

        if (!childSchema.getCollectionMethods().contains(Method.POST.toString())) {
            return;
        }

        Map<String, Object> requestParams = request.getRequestParams();
        if (!requestParams.containsKey(rel.getPropertyName())) {
            requestParams = new LinkedHashMap<String, Object>(requestParams);
            requestParams.put(rel.getPropertyName(), request.getId());
            request.setRequestParams(requestParams);
        }

        request.setType(childSchema.getId());
        request.setId(null);
        request.setLink(null);
    }

    public ObjectMetaDataManager getMetaDataManager() {
        return metaDataManager;
    }

    @Inject
    public void setMetaDataManager(ObjectMetaDataManager metaDataManager) {
        this.metaDataManager = metaDataManager;
    }

}
