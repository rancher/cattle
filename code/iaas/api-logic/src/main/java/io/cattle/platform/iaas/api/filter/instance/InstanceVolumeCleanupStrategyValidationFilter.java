package io.cattle.platform.iaas.api.filter.instance;

import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.Map;

public class InstanceVolumeCleanupStrategyValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Override
    public String[] getTypes() {
        return InstanceConstants.CONTAINER_LIKE.toArray(new String[InstanceConstants.CONTAINER_LIKE.size()]);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Instance.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        Map<?, ?> labels = DataUtils.getFieldFromRequest(request, InstanceConstants.FIELD_LABELS, Map.class);
        if (labels != null) {
            Object l = labels.get(SystemLabels.LABEL_VOLUME_CLEANUP_STRATEGY);
            if (l != null && !InstanceConstants.VOLUME_REMOVE_STRATEGIES.contains(l)) {
                throw new ClientVisibleException(ResponseCodes.BAD_REQUEST, ValidationErrorCodes.INVALID_OPTION, String.format(
                        "%s is an invalid value for the %s label.", l.toString(), SystemLabels.LABEL_VOLUME_CLEANUP_STRATEGY), null);
            }
        }
        
        return super.create(type, request, next);
    }

}