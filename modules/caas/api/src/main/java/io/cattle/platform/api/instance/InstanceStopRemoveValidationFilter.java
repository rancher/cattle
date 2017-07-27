package io.cattle.platform.api.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.AbstractValidationFilter;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;
import org.apache.commons.lang3.StringUtils;

public class InstanceStopRemoveValidationFilter extends AbstractValidationFilter {

    ObjectManager objectManager;

    public InstanceStopRemoveValidationFilter(ObjectManager objectManager) {
        super();
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request, ActionHandler next) {
        Instance instance = objectManager.loadResource(Instance.class, request.getId());
        if (request.getAction().equals("stop")) {
            setStopSource(instance, request);
        }

        return super.perform(name, obj, request, next);
    }

    protected void setStopSource(Instance instance, ApiRequest request) {
        String stopSource = DataAccessor.getFieldFromRequest(request, InstanceConstants.FIELD_STOP_SOURCE,
                String.class);
        if (!StringUtils.isBlank(stopSource)) {
            objectManager.setFields(instance, InstanceConstants.FIELD_STOP_SOURCE, stopSource);
        }
    }
}
