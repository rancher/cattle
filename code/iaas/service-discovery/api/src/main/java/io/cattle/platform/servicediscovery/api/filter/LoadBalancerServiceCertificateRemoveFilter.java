package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.LoadBalancerTable.LOAD_BALANCER;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerCertificateMap;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

public class LoadBalancerServiceCertificateRemoveFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;
    @Inject
    GenericMapDao mapDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Certificate.class };
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        Certificate cert = objectManager.loadResource(Certificate.class, id);
        List<String> serviceNames = new ArrayList<>();
        List<? extends LoadBalancerCertificateMap> mapsToRemove = mapDao.findNonRemoved(
                LoadBalancerCertificateMap.class,
                Certificate.class, cert.getId());
        for (LoadBalancerCertificateMap mapToRemove : mapsToRemove) {
            LoadBalancer lb = objectManager.findOne(LoadBalancer.class, LOAD_BALANCER.ID,
                    mapToRemove.getLoadBalancerId(), LOAD_BALANCER.REMOVED, null);
            if (lb == null) {
                continue;
            }
            Service service = objectManager.loadResource(Service.class, lb.getServiceId());
            if (service != null) {
                serviceNames.add(service.getName());
            }
        }
        if (!serviceNames.isEmpty()) {
            String serviceNameStr = StringUtils.join(serviceNames, ",");
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, ValidationErrorCodes.INVALID_ACTION,
                    "Certificate is in use by load balancer services: " + serviceNameStr, null);
        }

        return super.delete(type, id, request, next);
    }
}
