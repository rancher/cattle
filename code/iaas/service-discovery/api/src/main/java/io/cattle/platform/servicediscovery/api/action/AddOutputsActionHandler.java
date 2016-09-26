package io.cattle.platform.servicediscovery.api.action;

import io.cattle.platform.api.action.ActionHandler;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Stack;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class AddOutputsActionHandler implements ActionHandler {

    @Inject
    ObjectManager objectManager;

    @Override
    public String getName() {
        return ServiceConstants.TYPE_STACK + "." + ServiceConstants.ACTION_ADD_OUTPUTS;
    }

    @Override
    public Object perform(String name, Object obj, ApiRequest request) {
        if (!(obj instanceof Stack)) {
            return null;
        }
        Stack env = (Stack)obj;
        Map<String, Object> updates = new HashMap<>(DataAccessor.fieldMap(env, ServiceConstants.FIELD_OUTPUTS));
        updates.putAll(CollectionUtils.<String, Object>toMap(CollectionUtils.toMap(request.getRequestObject()).get(ServiceConstants.FIELD_OUTPUTS)));
        objectManager.setFields(obj, ServiceConstants.FIELD_OUTPUTS, updates);
        return objectManager.reload(env);
    }
}
