package io.github.ibuildthecloud.gdapi.request.handler;

import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.impl.CollectionImpl;
import io.github.ibuildthecloud.gdapi.model.impl.VersionImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;
import io.github.ibuildthecloud.gdapi.version.Versions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class VersionsHandler extends AbstractResponseGenerator {

    Versions versions;

    @Override
    protected void generate(ApiRequest request) throws IOException {
        if (request.getVersion() != null)
            return;

        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

        List<Resource> apiVersions = new ArrayList<Resource>();
        for (String version : versions.getVersions()) {
            apiVersions.add(new VersionImpl(version));
        }

        CollectionImpl collection = new CollectionImpl();
        collection.getLinks().put(UrlBuilder.LATEST, urlBuilder.version(versions.getLatest()));
        collection.getLinks().put(UrlBuilder.SELF, urlBuilder.current());
        collection.setData(apiVersions);
        collection.setResourceType(apiVersions.get(0).getType());

        request.setResponseObject(collection);
    }

    @Inject
    public void setVersions(Versions versions) {
        this.versions = versions;
    }

    public Versions getVersions() {
        return versions;
    }
}