package io.cattle.platform.api.machinedriver;

import io.cattle.platform.core.model.MachineDriver;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import org.apache.commons.lang3.StringUtils;

public class MachineDriverFilter extends AbstractValidationFilter {

    public static final String VERIFY_AGENT = "CantVerifyAgent";

    ObjectManager objectManager;

    public MachineDriverFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
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
