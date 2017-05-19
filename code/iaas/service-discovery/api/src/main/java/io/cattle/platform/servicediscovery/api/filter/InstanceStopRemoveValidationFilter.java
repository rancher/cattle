package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

@Named
public class InstanceStopRemoveValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] {
                InstanceConstants.KIND_CONTAINER,
                InstanceConstants.KIND_VIRTUAL_MACHINE
                };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        Instance instance = objectManager.loadResource(Instance.class, request.getId());
        if (request.getAction().equals("stop")) {
            setStopSource(instance, request);
        }

        return super.resourceAction(type, request, next);
    }

    protected void setStopSource(Instance instance, ApiRequest request) {
        String stopSource = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_STOP_SOURCE,
                String.class);
        if (StringUtils.isBlank(stopSource)) {
            if ("v1".equals(request.getVersion())) {
                objectManager.setFields(instance, InstanceConstants.FIELD_STOP_SOURCE, InstanceConstants.ACTION_SOURCE_EXTERNAL);
            }
        } else {
            objectManager.setFields(instance, InstanceConstants.FIELD_STOP_SOURCE, stopSource);
        }
    }
}
