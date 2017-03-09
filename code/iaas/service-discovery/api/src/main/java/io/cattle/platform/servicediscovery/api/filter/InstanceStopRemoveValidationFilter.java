package io.cattle.platform.servicediscovery.api.filter;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InstanceStopRemoveValidationFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;

    @Override
    public String[] getTypes() {
        return new String[] { "container" };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        Instance instance = objectManager.loadResource(Instance.class, request.getId());
        if (request.getAction().equals("stop")) {
            setStopSource(instance);
        } else if (request.getAction().equals("remove")) {
            setRemoveSource(instance);
        }

        return super.resourceAction(type, request, next);
    }

    protected void setRemoveSource(Instance instance) {
        Map<String, Object> data = new HashMap<>();
        data.put(InstanceConstants.FIELD_REMOVE_SOURCE, InstanceConstants.ACTION_SOURCE_API);
        objectManager.setFields(instance, data);
    }

    protected void setStopSource(Instance instance) {
        Map<String, Object> data = new HashMap<>();
        data.put(InstanceConstants.FIELD_STOP_SOURCE, InstanceConstants.ACTION_SOURCE_API);
        objectManager.setFields(instance, data);
    }
}
