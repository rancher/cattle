package io.cattle.platform.docker.machine.api;

import static io.cattle.platform.core.constants.MachineConstants.*;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.MachineConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.iaas.api.infrastructure.InfrastructureAccessManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class MachineLinkFilter implements ResourceOutputFilter {

    @Inject
    InfrastructureAccessManager infraAccess;

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        boolean add = false;
        if (original instanceof PhysicalHost || original instanceof Host) {
            if (StringUtils.isNotEmpty((String) DataUtils.getFields(original).get(EXTRACTED_CONFIG_FIELD))) {
                add = infraAccess.canModifyInfrastructure(ApiUtils.getPolicy());
            }
            if (!add && original instanceof Host && StringUtils.isNotEmpty((String) converted.getFields().get(MachineConstants.FIELD_DRIVER))) {
                add = infraAccess.canModifyInfrastructure(ApiUtils.getPolicy());
            }
        }

        if (add) {
            converted.getLinks().put(CONFIG_LINK, ApiContext.getUrlBuilder().resourceLink(converted, CONFIG_LINK));
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { KIND_MACHINE, HostConstants.TYPE };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}