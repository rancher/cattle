package io.github.ibuildthecloud.gdapi.doc.handler;

import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class DocumentationHandler extends AbstractNoOpResourceManager {

    Map<String, TypeDocumentation> docs = new TreeMap<String, TypeDocumentation>();

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { TypeDocumentation.class };
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new ArrayList<Object>(docs.values());
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        return docs.get(id);
    }

    @Override
    protected Object collectionActionInternal(Object resources, ApiRequest request) {
        return null;
    }

    public Map<String, TypeDocumentation> getDocs() {
        return docs;
    }

}
