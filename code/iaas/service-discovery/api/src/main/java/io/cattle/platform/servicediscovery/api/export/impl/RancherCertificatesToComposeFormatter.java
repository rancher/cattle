package io.cattle.platform.servicediscovery.api.export.impl;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class RancherCertificatesToComposeFormatter extends AbstractJooqDao
        implements RancherConfigToComposeFormatter {

    @Inject
    ObjectManager objManager;

    @Override
    @SuppressWarnings("unchecked")
    public Object format(ServiceDiscoveryConfigItem item, Object valueToTransform) {
        if (item.getDockerName().equalsIgnoreCase(ServiceDiscoveryConfigItem.CERTIFICATES.getDockerName())) {
            List<Integer> certificateIds = (List<Integer>) valueToTransform;
            List<String> certificateNames = new ArrayList<>();
            for (Integer certificateId : certificateIds) {
                certificateNames.add(getCertName(certificateId));
            }
            return certificateNames;
        } else if (item.getDockerName().equals(ServiceDiscoveryConfigItem.DEFAULT_CERTIFICATE.getDockerName())) {
            Integer defaultCertId = (Integer) valueToTransform;
            return getCertName(defaultCertId);
        } else {
            return null;
        }
    }

    private String getCertName(Integer certId) {
        return objManager.loadResource(Certificate.class, certId.longValue()).getName();
    }
}
