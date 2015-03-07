package io.cattle.platform.docker.machine.api;

import static io.cattle.platform.docker.machine.constants.MachineConstants.*;
import io.cattle.platform.core.model.PhysicalHost;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;

import org.apache.commons.lang.StringUtils;

public class MachineLinkFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if ( original instanceof PhysicalHost ) {
            PhysicalHost host = (PhysicalHost)original;
            if (StringUtils.isNotEmpty((String)DataUtils.getFields(host).get(EXTRACTED_CONFIG_FIELD))) {
                converted.getLinks().put(CONFIG_LINK, ApiContext.getUrlBuilder().resourceLink(converted, CONFIG_LINK));
            }
        }

        return converted;
    }

    @Override
    public String[] getTypes() {
        return new String[] { MACHINE_KIND };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[0];
    }

}
