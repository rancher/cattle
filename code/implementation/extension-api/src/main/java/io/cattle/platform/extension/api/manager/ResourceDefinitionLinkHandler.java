package io.cattle.platform.extension.api.manager;

import io.cattle.platform.extension.api.dot.DotMaker;
import io.cattle.platform.extension.api.model.ProcessDefinitionApi;
import io.cattle.platform.extension.api.model.ResourceDefinition;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.LinkHandler;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class ResourceDefinitionLinkHandler implements LinkHandler {

    public static final String PROCESSES = "processes";
    public static final String RESOURCE_DOT = "resourceDot";

    ResourceManagerLocator locator;
    DotMaker dotMaker;

    public ResourceDefinitionLinkHandler(ResourceManagerLocator locator, DotMaker dotMaker) {
        super();
        this.locator = locator;
        this.dotMaker = dotMaker;
    }

    @Override
    public boolean handles(String type, String id, String link, ApiRequest request) {
        return true;
    }

    @Override
    public Object link(String link, Object obj, ApiRequest request) throws IOException {
        String type = request.getSchemaFactory().getSchemaName(ProcessDefinitionApi.class);
        ResourceManager rm = locator.getResourceManagerByType(type);
        ResourceDefinition def = (ResourceDefinition) obj;

        if (link.equalsIgnoreCase(PROCESSES)) {
            Map<Object, Object> criteria = CollectionUtils.asMap((Object) "resourceType", def.getId());
            return CollectionUtils.toList(rm.list(type, criteria, new ListOptions()));
        } else if (request != null && link.equalsIgnoreCase(RESOURCE_DOT)) {
            String dot = dotMaker.getResourceDot(def.getName());

            boolean written = false;
            try {
                written = dotMaker.writeResponse(dot, request);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            return written ? Collections.emptyList() : null;
        }

        return null;
    }

}
