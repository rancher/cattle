package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.model.impl.VersionImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class VersionHandler extends AbstractResponseGenerator {

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (request.getType() == null && request.getVersion() != null) {
            UrlBuilder urlBuilder = ApiContext.getUrlBuilder();
            SchemaFactory schemaFactory = ApiContext.getSchemaFactory();
            VersionImpl version = new VersionImpl(request.getVersion());

            Map<String, URL> links = new TreeMap<String, URL>();
            version.setLinks(links);
            links.put(UrlBuilder.SELF, urlBuilder.current());

            for (Schema schema : schemaFactory.listSchemas()) {
                if (!schema.getCollectionMethods().contains(Method.GET.toString())) {
                    continue;
                }

                URL link = urlBuilder.resourceCollection(schema.getId());
                if (link != null) {
                    links.put(schema.getPluralName(), link);
                }
            }

            request.setResponseObject(version);
        }
    }

}
