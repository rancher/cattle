package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.addon.ToServiceUpgradeStrategy;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ServiceOutputFilter implements ResourceOutputFilter {

    @Inject
    JsonMapper jsonMapper;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {

        converted = convertUpgradeField(original, converted);
        return converted;
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
