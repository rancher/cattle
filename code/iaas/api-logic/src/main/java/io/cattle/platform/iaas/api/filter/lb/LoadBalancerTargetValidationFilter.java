package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerTarget;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerTargetValidationFilter extends AbstractDefaultResourceManagerFilter {

    private static final Map<String, Boolean> ACTIONS;

    static {
        ACTIONS = new HashMap<>();
        ACTIONS.put(LoadBalancerConstants.ACTION_LB_ADD_TARGET, true);
        ACTIONS.put(LoadBalancerConstants.ACTION_LB_REMOVE_TARGET, false);
    }

    @Inject
    LoadBalancerTargetDao lbTargetDao;

    @Inject
    LoadBalancerFilterUtils lbFilterUtils;

    @Inject
    GenericMapDao mapDao;

    @Inject
    JsonMapper jsonMapper;

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
        if (ACTIONS.containsKey(request.getAction())) {
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            LoadBalancerTargetInput lbTarget = DataAccessor.fromMap(data).withKey(
                    LoadBalancerConstants.FIELD_LB_TARGET).as(jsonMapper, LoadBalancerTargetInput.class);

            validateInstanceAndIp(request, lbTarget);
            validatePorts(lbTarget);
        }

        return super.resourceAction(type, request, next);
    }

    protected void validatePorts(LoadBalancerTargetInput lbTarget) {
        for (String port : lbTarget.getPorts()) {
            // just to validate the port
            new LoadBalancerTargetPortSpec(port);
        }
    }

    protected void validateInstanceAndIp(ApiRequest request, LoadBalancerTargetInput lbTarget) {
        // InstanceId and ipAddress are mutually exclusive; one of them have
        // to be specified
        boolean isInstance = lbTarget.getInstanceId() != null;
        boolean isIp = lbTarget.getIpAddress() != null;
        if (isInstance == isIp) {
            if (isInstance) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION, LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);
            } else {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.MISSING_REQUIRED, LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID);
            }
        }

        if (isInstance) {
            lbFilterUtils.validateGenericMapAction(mapDao, LoadBalancerTarget.class, Instance.class, lbTarget
                    .getInstanceId(), LoadBalancer.class, Long.valueOf(request
                    .getId()), new SimpleEntry<String, Boolean>(LoadBalancerConstants.FIELD_LB_TARGET_INSTANCE_ID, ACTIONS.get(request.getAction())));
        } else {
            validateIpMapAction(request, lbTarget);
        }
    }

    private void validateIpMapAction(ApiRequest request, LoadBalancerTargetInput lbTarget) {
        LoadBalancerTarget target = lbTargetDao.getLoadBalancerTarget(Long.valueOf(request.getId()), lbTarget);
        if (ACTIONS.get(request.getAction())) {
            if (target != null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);
            }
        } else {
            if (target == null) {
                ValidationErrorCodes.throwValidationError(ValidationErrorCodes.INVALID_OPTION, LoadBalancerConstants.FIELD_LB_TARGET_IPADDRESS);
            }
        }
    }
}
