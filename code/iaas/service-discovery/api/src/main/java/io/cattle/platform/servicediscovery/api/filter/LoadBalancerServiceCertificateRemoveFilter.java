package io.cattle.platform.servicediscovery.api.filter;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.servicediscovery.api.dao.ServiceDao;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.lang.StringUtils;

public class LoadBalancerServiceCertificateRemoveFilter extends AbstractDefaultResourceManagerFilter {
    @Inject
    ObjectManager objectManager;
    @Inject
    ServiceDao svcDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Certificate.class };
    }

    @Override
    public Object delete(String type, String id, ApiRequest request, ResourceManager next) {
        validateIfCertificateInUse(id);

        return super.delete(type, id, request, next);
    }

    @SuppressWarnings("unchecked")
    protected void validateIfCertificateInUse(String certificateId) {
        Certificate cert = objectManager.loadResource(Certificate.class, certificateId);
        List<String> serviceNames = new ArrayList<>();
        List<Service> lbServices = new ArrayList<>();
        List<String> types = Arrays.asList(ServiceConstants.KIND_LOAD_BALANCER_SERVICE);
        for (String type : types) {
            lbServices.addAll(objectManager.find(Service.class, SERVICE.ACCOUNT_ID, cert.getAccountId(),
                SERVICE.REMOVED, null, SERVICE.KIND, type));
        }
        for (Service lbService : lbServices) {
            List<Long> certIds = (List<Long>) CollectionUtils.collect(
                    svcDao.getLoadBalancerServiceCertificates(lbService),
                    TransformerUtils.invokerTransformer("getId"));
            Certificate defaultCert = svcDao.getLoadBalancerServiceDefaultCertificate(lbService);
            if (defaultCert != null) {
                certIds.add(defaultCert.getId());
            }
            if (certIds.contains(cert.getId())) {
                serviceNames.add(lbService.getName());
            }
        }
        if (!serviceNames.isEmpty()) {
            String serviceNameStr = StringUtils.join(serviceNames, ",");
            throw new ClientVisibleException(ResponseCodes.METHOD_NOT_ALLOWED, ValidationErrorCodes.INVALID_ACTION,
                    "Certificate is in use by load balancer services: " + serviceNameStr, null);
        }
    }

    @Override
    public Object resourceAction(String type, ApiRequest request, ResourceManager next) {
        if (request.getAction().equalsIgnoreCase("remove")) {
            validateIfCertificateInUse(request.getId());
        }

        return super.resourceAction(type, request, next);
    }
}
