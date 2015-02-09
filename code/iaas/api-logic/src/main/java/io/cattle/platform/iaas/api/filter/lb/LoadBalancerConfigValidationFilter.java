package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerConfigListenerMap;
import io.cattle.platform.core.model.LoadBalancerListener;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class LoadBalancerConfigValidationFilter extends AbstractDefaultResourceManagerFilter {

    private static final Map<String, Boolean> actions;
    static
    {
        actions = new HashMap<>();
        actions.put(LoadBalancerConstants.ACTION_LB_CONFIG_ADD_LISTENER, true);
        actions.put(LoadBalancerConstants.ACTION_LB_CONFIG_REMOVE_LISTENER, false);
    }

    @Inject
    LoadBalancerDao lbDao;

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
        return new Class<?>[] { LoadBalancerConfig.class };
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        List<? extends LoadBalancer> lbs = lbDao.listByConfigId(Long.valueOf(id));
        if (!lbs.isEmpty()) {
            throw new ClientVisibleException(ResponseCodes.CONFLICT, "LoadBalancerConfigIsInUse");
        }

        return super.delete(type, id, request, next);
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (actions.containsKey(request.getAction())) {
            Map<String, Object> data = CollectionUtils.toMap(request.getRequestObject());
            Long listenerId = (Long) data.get(LoadBalancerConstants.FIELD_LB_LISTENER_ID);

            lbFilterUtils.validateGenericMapAction(
                    mapDao,
                    LoadBalancerConfigListenerMap.class,
                    LoadBalancerListener.class,
                    listenerId,
                    LoadBalancerConfig.class,
                    Long.valueOf(request.getId()),
                    new SimpleEntry<String, Boolean>(LoadBalancerConstants.FIELD_LB_LISTENER_ID, actions.get(request
                            .getAction())));

        }

        return super.resourceAction(type, request, next);
    }
}
