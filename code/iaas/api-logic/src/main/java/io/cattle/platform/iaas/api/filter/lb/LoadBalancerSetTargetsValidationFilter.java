package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.util.LoadBalancerTargetPortSpec;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerSetTargetsValidationFilter extends AbstractDefaultResourceManagerFilter {
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
        if (request.getAction().equalsIgnoreCase(LoadBalancerConstants.ACTION_LB_SET_TARGETS)) {
            List<? extends LoadBalancerTargetInput> newLBTargets = DataAccessor
                    .fromMap(request.getRequestObject())
                    .withKey(
                            LoadBalancerConstants.FIELD_LB_TARGETS).asList(jsonMapper, LoadBalancerTargetInput.class);
            if (newLBTargets != null) {
                for (LoadBalancerTargetInput newLBTarget : newLBTargets) {
                    List<? extends String> ports = newLBTarget.getPorts();
                    for (String port : ports) {
                        // validate the spec
                        new LoadBalancerTargetPortSpec(port);
                    }
                }
            }
        }

        return super.resourceAction(type, request, next);
    }
}
