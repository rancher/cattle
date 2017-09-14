package io.cattle.platform.api.stack;

import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ActionHandler;

import java.util.HashMap;
import java.util.Map;

public class AddOutputsActionHandler implements ActionHandler {

    ObjectManager objectManager;

    public AddOutputsActionHandler(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public Object perform(Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack env = (Stack)obj;
        Map<String, Object> updates = new HashMap<>(DataAccessor.fieldMap(env, ServiceConstants.FIELD_OUTPUTS));
        updates.putAll(CollectionUtils.toMap(CollectionUtils.toMap(request.getRequestObject()).get(ServiceConstants.FIELD_OUTPUTS)));
        objectManager.setFields(obj, ServiceConstants.FIELD_OUTPUTS, updates);
        return objectManager.reload(env);
    }
}
