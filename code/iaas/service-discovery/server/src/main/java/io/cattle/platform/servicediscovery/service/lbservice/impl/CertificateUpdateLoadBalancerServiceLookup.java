package io.cattle.platform.servicediscovery.service.lbservice.impl;

import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.servicediscovery.service.lbservice.LoadBalancerServiceLookup;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

public class CertificateUpdateLoadBalancerServiceLookup implements LoadBalancerServiceLookup {
    @Inject
    ObjectManager objMgr;
    @Inject
    JsonMapper jsonMapper;

    @Override
    public List<? extends Service> getLoadBalancerServices(Object obj) {
        if (!(obj instanceof Certificate)) {
            return null;
        }
        Certificate cert = (Certificate) obj;
        List<Service> lbServices = objMgr.find(Service.class, SERVICE.ACCOUNT_ID, cert.getAccountId(), SERVICE.KIND,
                ServiceConstants.KIND_LOAD_BALANCER_SERVICE, SERVICE.REMOVED, null);
        Iterator<Service> it = lbServices.iterator();
        while (it.hasNext()) {
            Service lbSvc = it.next();
            List<? extends Long> certIds = DataAccessor.fields(lbSvc)
                    .withKey(LoadBalancerConstants.FIELD_LB_CERTIFICATE_IDS).withDefault(Collections.EMPTY_LIST)
                    .asList(jsonMapper, Long.class);
            Long defaultCertId = DataAccessor.fieldLong(lbSvc, LoadBalancerConstants.FIELD_LB_DEFAULT_CERTIFICATE_ID);
            if (!(certIds.contains(cert.getId()) || (defaultCertId != null && defaultCertId.equals(cert.getId())))) {
                it.remove();
            }
        }

        return lbServices;
    }
}
