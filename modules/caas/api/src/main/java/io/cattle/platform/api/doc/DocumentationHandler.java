package io.cattle.platform.api.doc;

import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DocumentationHandler extends AbstractNoOpResourceManager {

    List<URL> resources;
    JsonMapper jsonMapper;
    Map<String, TypeDocumentation> docs = new TreeMap<>();

    public DocumentationHandler(JsonMapper jsonMapper, List<URL> resources) throws IOException {
        this.resources = resources;
        this.jsonMapper = jsonMapper;
        init();
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        return new ArrayList<Object>(docs.values());
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        return docs.get(id);
    }

    public Map<String, TypeDocumentation> getDocs() {
        return docs;
    }

    private void init() throws IOException {
        for (URL url : resources) {
            InputStream is = url.openStream();

            try {
                Map<String, TypeDocumentation> typeDocs = getDocs();
                List<TypeDocumentation> docs = jsonMapper.readCollectionValue(is, List.class, TypeDocumentation.class);

                for (TypeDocumentation doc : docs) {
                    typeDocs.put(doc.getId(), doc);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

}
