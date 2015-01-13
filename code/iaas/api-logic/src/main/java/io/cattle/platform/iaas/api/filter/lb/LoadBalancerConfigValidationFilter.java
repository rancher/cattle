package io.cattle.platform.iaas.api.filter.lb;

import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;

import java.util.List;

import javax.inject.Inject;

public class LoadBalancerConfigValidationFilter extends AbstractDefaultResourceManagerFilter {

    LoadBalancerDao lbDao;

    @Override
    public String[] getTypes() {
        return new String[] { LoadBalancerConstants.OBJECT_LB_CONFIG };
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

    @Inject
    public void setLoadBalancerDao(LoadBalancerDao lbDao) {
        this.lbDao = lbDao;
    }
}
