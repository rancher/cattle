package io.cattle.platform.lb.instance.service.impl;

import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.tables.records.LoadBalancerCertificateMapRecord;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

public class CertificateMapLoadBalancerLookup implements LoadBalancerLookup {

    @Inject
    LoadBalancerDao lbDao;

    @Override
    public Set<Long> getLoadBalancerIds(Object obj) {
        Set<Long> lbIds = new HashSet<>();
        if (!(obj instanceof LoadBalancerCertificateMapRecord)) {
            return lbIds;
        }

        LoadBalancerCertificateMapRecord lbCertMap = (LoadBalancerCertificateMapRecord) obj;
        LoadBalancer lb = lbDao.getActiveLoadBalancerById(lbCertMap.getLoadBalancerId());
        if (lb != null) {
            lbIds.add(lb.getId());
        }
        return lbIds;
    }
}