package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

public class LoadBalancerListenerValidationFilter extends AbstractDefaultResourceManagerFilter {

    @Override
    public String[] getTypes() {
        return new String[] { LoadBalancerConstants.OBJECT_LB_LISTENER };
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { LoadBalancerListener.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        LoadBalancerListener listener = request.proxyRequestObject(LoadBalancerListener.class);
        Integer targetPort = listener.getTargetPort();
        Integer privatePort = listener.getPrivatePort();
        Integer sourcePort = listener.getSourcePort();

        if (privatePort == null && sourcePort == null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MISSING_REQUIRED,
                    LoadBalancerConstants.FIELD_LB_SOURCE_PORT);
        }

        if (privatePort == null) {
            privatePort = sourcePort;
        }

        if (targetPort == null) {
            targetPort = privatePort;
        }

        listener.setTargetPort(targetPort);
        listener.setSourcePort(sourcePort);
        listener.setPrivatePort(privatePort);

        String targetProtocol = listener.getTargetProtocol();
        if (targetProtocol == null) {
            String sourceProtocol = listener.getSourceProtocol();
            listener.setTargetProtocol(sourceProtocol);
        }

        return super.create(type, request, next);
    }
}
