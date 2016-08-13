package io.cattle.platform.iaas.api.filter.machinedriver;

import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class MachineDriverFilter extends AbstractDefaultResourceManagerFilter {

    public static final String VERIFY_AGENT = "CantVerifyAgent";

    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { "machineDriver" };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String url = DataAccessor.fromMap(request.getRequestObject()).withKey("url").as(String.class);
        MachineDriver md = request.proxyRequestObject(MachineDriver.class);
        
        if (url != null && StringUtils.isBlank(md.getName())) {
            String[] parts = url.split("/");
            String name = parts[parts.length-1];
            name = StringUtils.removeStart(name, "docker-machine-driver-");
            name = StringUtils.removeStart(name, "docker-machine-");
            name = name.split("[^a-zA-Z0-9]")[0];
            md.setName(name);
        }

        return super.create(type, request, next);
    }
}
