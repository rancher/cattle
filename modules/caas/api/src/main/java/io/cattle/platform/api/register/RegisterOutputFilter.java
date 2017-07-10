package io.cattle.platform.api.register;

import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.GenericObjectConstants;
import io.cattle.platform.core.constants.RegisterConstants;
import io.cattle.platform.core.model.GenericObject;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.response.ResourceOutputFilter;
import io.github.ibuildthecloud.gdapi.util.RequestUtils;

public class RegisterOutputFilter implements ResourceOutputFilter {

    @Override
    public Resource filter(ApiRequest request, Object original, Resource converted) {
        if (!(original instanceof GenericObject)) {
            return converted;
        }

        Object key = request == null ? null :
            RequestUtils.makeSingularIfCan(request.getRequestParams().get(GenericObjectConstants.FIELD_KEY));

        if (!CommonStatesConstants.ACTIVE.equals(((GenericObject) original).getState()) ||
                key == null || !key.equals(((GenericObject) original).getKey())) {
            converted.getFields().remove(RegisterConstants.FIELD_ACCESS_KEY);
            converted.getFields().remove(RegisterConstants.FIELD_SECRET_KEY);
            converted.getFields().remove(GenericObjectConstants.FIELD_KEY);
        }

        return converted;
    }

}
