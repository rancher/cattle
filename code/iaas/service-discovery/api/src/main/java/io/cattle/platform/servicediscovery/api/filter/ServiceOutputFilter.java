package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ServiceUpgrade;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.api.constants.ServiceDiscoveryConstants;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import javax.inject.Inject;

public class ServiceOutputFilter implements ResourceOutputFilter {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        String type = ApiContext.getSchemaFactory().getSchemaName(Service.class);
        ServiceUpgrade upgrade = DataAccessor.field(original, ServiceDiscoveryConstants.FIELD_UPGRADE, jsonMapper,
                ServiceUpgrade.class);

        if (upgrade == null || type == null || upgrade.getToServiceId() == null) {
            return converted;
        }

        IdFormatter formatter = ApiContext.getContext().getIdFormatter();
        upgrade.setToServiceId(formatter.formatId(type, upgrade.getToServiceId()).toString());
        converted.getFields().put(ServiceDiscoveryConstants.FIELD_UPGRADE, upgrade);

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[]{ServiceDiscoveryConstants.KIND.SERVICE.toString().toLowerCase()};
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[]{Service.class};
    }
}
