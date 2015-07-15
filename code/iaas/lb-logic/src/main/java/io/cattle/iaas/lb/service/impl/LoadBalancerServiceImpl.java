package io.cattle.iaas.lb.service.impl;

import static io.cattle.platform.core.model.tables.LoadBalancerCertificateMapTable.LOAD_BALANCER_CERTIFICATE_MAP;
import io.cattle.iaas.lb.service.LoadBalancerService;
import io.cattle.platform.core.addon.LoadBalancerCertificate;
import io.cattle.platform.core.addon.LoadBalancerTargetInput;
import io.cattle.platform.core.constants.LoadBalancerConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.LoadBalancerDao;
import io.cattle.platform.core.dao.LoadBalancerTargetDao;
import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.core.model.LoadBalancer;
import io.cattle.platform.core.model.LoadBalancerCertificateMap;
import io.cattle.platform.core.model.LoadBalancerConfig;
import io.cattle.platform.core.model.LoadBalancerHostMap;
import io.cattle.platform.deferred.util.DeferredUtils;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

public class LoadBalancerServiceImpl implements LoadBalancerService {

    @Inject
    GenericResourceDao resourceDao;

    @Inject
    LoadBalancerTargetDao lbTargetDao;
    
    @Inject
    LoadBalancerDao lbDao;

    @Inject
    GenericMapDao mapDao;

    @Inject
    ObjectProcessManager objectProcessMgr;

    @Override
    public void addListenerToConfig(final LoadBalancerConfig config, final long listenerId) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                lbDao.addListenerToConfig(config, listenerId);
            }
        });
    }

    @Override
    public void addTargetToLoadBalancer(final LoadBalancer lb, final LoadBalancerTargetInput targetInput) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                lbTargetDao.createLoadBalancerTarget(lb, targetInput);
            }
        });
    }

    @Override
    public void removeTargetFromLoadBalancer(final LoadBalancer lb, final LoadBalancerTargetInput toRemove) {
        DeferredUtils.nest(new Runnable() {
            @Override
            public void run() {
                lbTargetDao.removeLoadBalancerTarget(lb, toRemove);

            }
        });
    }

    @Override
    public LoadBalancerHostMap addHostWLaunchConfigToLoadBalancer(LoadBalancer lb, final Map<String, Object> data) {
        data.put(LoadBalancerConstants.FIELD_LB_ID, lb.getId());
        data.put("accountId", lb.getAccountId());
        return DeferredUtils.nest(new Callable<LoadBalancerHostMap>() {
            @Override
            public LoadBalancerHostMap call() throws Exception {
                return resourceDao.createAndSchedule(LoadBalancerHostMap.class, data);
            }
        });
    }

    protected void addCertificateToLoadBalancer(final LoadBalancer lb, final LoadBalancerCertificate cert) {
        LoadBalancerCertificateMap lbCertMap = mapDao.findNonRemoved(LoadBalancerCertificateMap.class,
                LoadBalancer.class, lb.getId(),
                Certificate.class, cert.getCertificateId());

        if (lbCertMap == null) {
            DeferredUtils.nest(new Runnable() {
                @Override
                public void run() {
                    resourceDao.createAndSchedule(LoadBalancerCertificateMap.class,
                            LOAD_BALANCER_CERTIFICATE_MAP.LOAD_BALANCER_ID,
                            lb.getId(), LOAD_BALANCER_CERTIFICATE_MAP.CERTIFICATE_ID, cert.getCertificateId(),
                            LOAD_BALANCER_CERTIFICATE_MAP.ACCOUNT_ID, lb.getAccountId(),
                            LOAD_BALANCER_CERTIFICATE_MAP.IS_DEFAULT, cert.isDefault());
                }
            });
        }
    }

    protected void removeCertificateFromLoadBalancer(final LoadBalancer lb, final LoadBalancerCertificate cert) {
        final LoadBalancerCertificateMap lbCertMap = mapDao.findToRemove(LoadBalancerCertificateMap.class,
                LoadBalancer.class, lb.getId(),
                Certificate.class, cert.getCertificateId());

        if (lbCertMap != null) {
            DeferredUtils.nest(new Runnable() {
                @Override
                public void run() {
                    objectProcessMgr.scheduleStandardProcess(StandardProcess.REMOVE, lbCertMap, null);
                }
            });
        }
    }

    @Override
    public void updateLoadBalancerCertificates(LoadBalancer lb, Set<LoadBalancerCertificate> newCertsSet) {
        List<? extends LoadBalancerCertificateMap> existingCerts = mapDao.findNonRemoved(
                LoadBalancerCertificateMap.class,
                LoadBalancer.class, lb.getId());
        Set<LoadBalancerCertificate> existingLbCerts = new HashSet<>();
        for (LoadBalancerCertificateMap existingCert : existingCerts) {
            existingLbCerts.add(new LoadBalancerCertificate(existingCert.getCertificateId(), existingCert
                    .getIsDefault()));
        }

        addMissingCertificates(lb, newCertsSet, existingLbCerts);

        removeExtraCertificates(lb, newCertsSet, existingLbCerts);
    }

    protected void removeExtraCertificates(LoadBalancer lb, Set<LoadBalancerCertificate> newCerts,
            Set<LoadBalancerCertificate> existingLbCerts) {
        Set<LoadBalancerCertificate> toRemove = new HashSet<>();
        toRemove.addAll(existingLbCerts);
        toRemove.removeAll(newCerts);
        for (LoadBalancerCertificate certToRemove : toRemove) {
            removeCertificateFromLoadBalancer(lb, certToRemove);
        }
    }

    protected void addMissingCertificates(LoadBalancer lb, Set<LoadBalancerCertificate> newCerts,
            Set<LoadBalancerCertificate> existingLbCerts) {
        Set<LoadBalancerCertificate> toAdd = new HashSet<>();
        toAdd.addAll(newCerts);
        toAdd.removeAll(existingLbCerts);
        for (LoadBalancerCertificate certToAdd : toAdd) {
            addCertificateToLoadBalancer(lb, certToAdd);
        }
    }
}
