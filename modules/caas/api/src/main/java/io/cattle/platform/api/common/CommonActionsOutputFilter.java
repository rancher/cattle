package io.cattle.platform.api.common;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class CommonActionsOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        Map<String, URL> links = converted.getLinks();
        Map<String, URL> actions = converted.getActions();

        URL selfLink = links.get(UrlBuilder.SELF);

        for (String name : new String[] {UrlBuilder.UPDATE, UrlBuilder.REMOVE}) {
            if (actions.containsKey(name)) {
                actions.remove(name);
                links.put(name, selfLink);
            }
        }

        actions.remove("error");
        actions.remove("create");

        if (request != null) {
            Schema schema = request.getSchemaFactory().getSchema(converted.getType());
            if (schema != null && contains(schema.getResourceMethods(), "PUT")) {
                links.put("update", selfLink);
            }
            if (schema != null && contains(schema.getResourceMethods(), "DELETE")) {
                links.put("remove", selfLink);
            }
        }

        return converted;
    }

    protected boolean contains(List<String> list, String item) {
        if (list == null) {
            return false;
        }
        return list.contains(item);
    }
}
