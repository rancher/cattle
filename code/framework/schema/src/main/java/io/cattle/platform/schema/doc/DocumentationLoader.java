package io.cattle.platform.schema.doc;

import io.cattle.platform.json.JsonMapper;
import io.github.ibuildthecloud.gdapi.doc.TypeDocumentation;
import io.github.ibuildthecloud.gdapi.doc.handler.DocumentationHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;

public class DocumentationLoader {

    DocumentationHandler docHandler;
    List<URL> resources;
    JsonMapper jsonMapper;

    @PostConstruct
    public void init() throws IOException {
        for (URL url : resources) {
            InputStream is = url.openStream();

            try {
                Map<String, TypeDocumentation> typeDocs = docHandler.getDocs();
                List<TypeDocumentation> docs = jsonMapper.readCollectionValue(is, List.class, TypeDocumentation.class);

                for (TypeDocumentation doc : docs) {
                    typeDocs.put(doc.getId(), doc);
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    public List<URL> getResources() {
        return resources;
    }

    public void setResources(List<URL> resources) {
        this.resources = resources;
    }

    public DocumentationHandler getDocHandler() {
        return docHandler;
    }

    @Inject
    public void setDocHandler(DocumentationHandler docHandler) {
        this.docHandler = docHandler;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }
}
