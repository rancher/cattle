package io.cattle.platform.iaas.api.cluster;

import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

// Since a cluster inherits from host and links are created automatically
// from inspecting the DB schema, we currently get links such as:
// host <=> host and cluster <=> cluster
// when the intended links are host <=> cluster and vice-versa.

// This filter just limits what is exposed for discovery.  It doesn't
// limit the user from hitting this API.  This should be fine since
// hitting this API still retrieves the correct results
public class HostClusterLinkFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (converted.getType().equals("cluster")) {
            converted.getLinks().remove("clusters");
        } else if (converted.getType().equals("host")) {
            converted.getLinks().remove("hosts");
        }
        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { "cluster", "host" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] {};
    }

}
