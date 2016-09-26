package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class HostOutputFilter implements ResourceOutputFilter {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {

        converted = convertPublicEndpointsField(original, converted);

        return converted;
    }

    public Resource convertPublicEndpointsField(Object original, Resource converted) {
        List<? extends PublicEndpoint> endpoints = DataAccessor.fields(original)
                .withKey(ServiceConstants.FIELD_PUBLIC_ENDPOINTS).withDefault(Collections.EMPTY_LIST)
                .asList(jsonMapper, PublicEndpoint.class);

        if (endpoints.isEmpty()) {
            return converted;
        }

        converted.getFields().put(ServiceConstants.FIELD_PUBLIC_ENDPOINTS,
                ServiceOutputFilter.getConvertedEndpoints(endpoints));
        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { "host" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Host.class };
    }
}
