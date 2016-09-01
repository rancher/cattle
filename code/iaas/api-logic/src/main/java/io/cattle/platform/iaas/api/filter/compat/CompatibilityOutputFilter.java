package io.cattle.platform.iaas.api.filter.compat;

import io.cattle.platform.core.model.Service;
import io.cattle.platform.object.util.ObjectUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompatibilityOutputFilter implements ResourceOutputFilter {

    private static final Logger log = LoggerFactory.getLogger(CompatibilityOutputFilter.class);
    
    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (request == null) {
            return converted;
        }

        if (!"v1".equals(request.getVersion())) {
            return converted;
        }

        switch(converted.getType()) {
        case "stack":
            mapStack(request, original, converted);
            break;
        case "service":
        case "kubernetesService":
        case "composeService":
        case "loadBalancerService":
        case "dnsService":
        case "externalService":
            mapService(request, original, converted);
        }
        return converted;
    }

    protected void mapStack(ApiRequest apiRequest, Object original, Resource converted) {
        if (converted instanceof ResourceImpl) {
            if ("stack".equals(converted.getType())) {
                IdFormatter idF = ApiContext.getContext().getIdFormatter();
                ((ResourceImpl)converted).setId(idF.formatId("environment", ObjectUtils.getId(original)).toString());
                ((ResourceImpl)converted).setType("environment");
            }
        }

        if ("stack".equals(converted.getFields().get("kind"))) {
            converted.getFields().put("kind", "environment");
        }
        convertURLs(converted, "/stacks/", "/environments/", converted.getLinks());
        convertURLs(converted, "/stacks/", "/environments/", converted.getActions());
    }
    
    protected void mapService(ApiRequest apiRequest, Object original, Resource converted) {
        UrlBuilder urlBuilder = apiRequest.getUrlBuilder();
        IdFormatter idF = ApiContext.getContext().getIdFormatter();

        if (original instanceof Service) {
            Long stackId = ((Service) original).getStackId();
            if (stackId != null) {
                converted.getFields().put("environmentId", idF.formatId("environment", stackId));
                converted.getLinks().put("environment", urlBuilder.resourceReferenceLink("environment", stackId.toString()));
                converted.getLinks().remove("stack");
            }
        }

        convertURLs(converted, "/stacks/", "/environments/", converted.getLinks());
        convertURLs(converted, "/stacks/", "/environments/", converted.getActions());
    }
    
    protected void convertURLs(Resource converted, String from, String to, Map<String, URL> links) {
        Map<String, URL> newLinks = new TreeMap<>();
        for (String key : links.keySet()) {
            URL value = links.get(key);
            if (value == null) {
                continue;
            }
            try {
                URL newValue = new URL(value.toString().replace(from, to));
                newLinks.put(key, newValue);
            } catch (MalformedURLException e) {
                log.error("Failed to build new URL for {}", value, e);
            }
        }
        links.putAll(newLinks);
    }
    
    @Override
    public String[] getTypes() {
        return new String[] { "stack", "service", "dnsService",
            "externalService", "loadBalancerService", "kubernetesService",
            "composeService" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

}
