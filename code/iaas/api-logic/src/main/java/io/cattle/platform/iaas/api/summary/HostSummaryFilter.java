package io.cattle.platform.iaas.api.summary;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.addon.HostSummary;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

public class HostSummaryFilter implements ResourceOutputFilter {

    @Inject
    ObjectMetaDataManager metaDataManager;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if ( ! (original instanceof HostSummary) ) {
            return converted;
        }

        SchemaFactory factory = request.getSchemaFactory();
        Schema schema = factory.getSchema(Host.class);
        UrlBuilder urlBuilder = ApiContext.getUrlBuilder();

        HostSummary summary = (HostSummary)original;
        Long hostId = summary.getHostId();

        if ( schema == null || hostId == null ) {
            return converted;
        }

        String hostIdStr = ApiContext.getContext().getIdFormatter().formatId(schema.getId(), hostId).toString();

        Map<String, URL> links = converted.getLinks();
        links.put("host", urlBuilder.resourceReferenceLink(Host.class, hostId.toString()));
        links.put("instances", urlBuilder.resourceLink(Host.class, hostId.toString(), "instances"));

        Resource hostResource = new ResourceImpl(hostIdStr, schema.getId(), Collections.<String,Object>emptyMap());
        ApiUtils.addActions(summary.getState(), metaDataManager.getActionDefinitions(Host.class), original, factory, schema, hostResource);

        converted.getActions().putAll(hostResource.getActions());

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{ HostSummary.class };
    }

}
