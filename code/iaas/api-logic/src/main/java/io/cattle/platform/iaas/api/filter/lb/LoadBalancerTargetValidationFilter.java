package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerTargetValidationFilter extends AbstractDefaultResourceManagerFilter {
    private static final Map<String, Boolean> actions;
    static
    {
        actions = new HashMap<>();
        actions.put(LoadBalancerConstants.ACTION_LB_ADD_TARGET, true);
        actions.put(LoadBalancerConstants.ACTION_LB_REMOVE_TARGET, false);
    }

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Inject
    LoadBalancerFilterUtils lbFilterUtils;

    @Inject
    GenericMapDao mapDao;

    @Override
    public String[] getTypes() {
        return new String[0];
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { LoadBalancer.class };
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (actions.containsKey(request.getAction())) {
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            Long instanceId = (Long) data.get(LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID);
            String ipAddress = (String) data.get(LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);

            boolean isInstance = (instanceId != null);
            boolean isIp = (ipAddress != null);

            // InstanceId and ipAddress are mutually exclusive; one of them have to be specified
            if (isInstance == isIp) {
                if (isInstance) {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                            LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);
                } else {
                    ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MISSING_REQUIRED,
                            LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID);
                }
            }

            if (isInstance) {
                lbFilterUtils.validateGenericMapAction(
                        mapDao,
                        LoadBalancerTarget.class,
                        Instance.class,
                        instanceId,
                        LoadBalancer.class,
                        Long.valueOf(request.getId()),
                        new SimpleEntry<String, Boolean>(LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID, actions
                                .get(request
                                        .getAction())));
            } else {
                validateIpMapAction(request, ipAddress);
            }
        }

        return super.resourceAction(type, request, next);
    }

    private void validateIpMapAction(ApiRequest request, String ipAddress) {
        LoadBalancerTarget target = lbTargetDao.getLbIpAddressTarget(Long.valueOf(request.getId()), ipAddress);
        if (actions.get(request.getAction())) {
            if (target != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE,
                        LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);
            }
        } else {
            if (target == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION,
                        LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);
            }
        }
    }
}
