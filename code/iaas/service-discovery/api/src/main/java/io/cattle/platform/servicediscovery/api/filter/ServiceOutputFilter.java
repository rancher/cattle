package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.PublicEndpoint;
import io.cattle.platform.core.addon.ToServiceUpgradeStrategy;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class ServiceOutputFilter implements ResourceOutputFilter {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {

        converted = convertUpgradeField(original, converted);
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

        converted.getFields().put(ServiceConstants.FIELD_PUBLIC_ENDPOINTS, getConvertedEndpoints(endpoints));
        return converted;
    }

    public static List<? extends PublicEndpoint> getConvertedEndpoints(List<? extends PublicEndpoint> endpoints) {
        List<PublicEndpoint> convertedEndpoints = new ArrayList<>();
        IdFormatter formatter = ApiContext.getContext().getIdFormatter();
        for (PublicEndpoint endpoint : endpoints) {
            if (endpoint.getHostId() != null) {
                String type = ApiContext.getSchemaFactory().getSchemaName(Host.class);
                endpoint.setHostId(formatter.formatId(type, endpoint.getHostId()).toString());
            }
            
            if (endpoint.getServiceId() != null) {
                String type = ApiContext.getSchemaFactory().getSchemaName(Service.class);
                endpoint.setServiceId(formatter.formatId(type, endpoint.getServiceId()).toString());
            }

            if (endpoint.getInstanceId() != null) {
                String type = ApiContext.getSchemaFactory().getSchemaName(Instance.class);
                endpoint.setInstanceId(formatter.formatId(type, endpoint.getInstanceId())
                        .toString());
            }
            convertedEndpoints.add(endpoint);
        }
        return convertedEndpoints;
    }

    public Resource convertUpgradeField(Object original, Resource converted) {
        String type = ApiContext.getSchemaFactory().getSchemaName(Service.class);
        if (type == null) {
            return converted;
        }
        io.cattle.platform.core.addon.ServiceUpgrade upgrade = DataAccessor.field(original,
                ServiceConstants.FIELD_UPGRADE, jsonMapper,
                io.cattle.platform.core.addon.ServiceUpgrade.class);

        if (upgrade == null || upgrade.getToServiceStrategy() == null) {
            return converted;
        }

        ToServiceUpgradeStrategy toServiceStrategy = upgrade.getToServiceStrategy();
        if (toServiceStrategy.getToServiceId() == null) {
            return converted;
        }

        IdFormatter formatter = ApiContext.getContext().getIdFormatter();
        toServiceStrategy.setToServiceId(formatter.formatId(type, toServiceStrategy.getToServiceId()).toString());
        upgrade.setToServiceStrategy(toServiceStrategy);
        converted.getFields().put(ServiceConstants.FIELD_UPGRADE, upgrade);
        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[]{ServiceConstants.KIND_SERVICE};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Service.class};
    }
}
