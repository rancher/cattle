package io.cattle.platform.iaas.api.filter.settings;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.impl.ResourceImpl;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.url.UrlBuilder;

public class SettingsOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource r) {
        if (r instanceof ResourceImpl) {
            ((ResourceImpl) r).setType("setting");
            Object id = r.getFields().get("name");
            if (id != null) {
                ((ResourceImpl) r).setId(id.toString());
            }
            r.getLinks().remove(UrlBuilder.SELF);
            // Recreate the SELF url
            r.getLinks();
            ((ResourceImpl) r).setType("activeSetting");
            ((ResourceImpl) r).getFields().put("baseType", "setting");
        }
        return r;
    }

}
