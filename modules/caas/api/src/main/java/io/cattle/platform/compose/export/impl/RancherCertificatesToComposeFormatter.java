package io.cattle.platform.compose.export.impl;

import io.cattle.platform.core.model.Certificate;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Configuration;

public class RancherCertificatesToComposeFormatter extends AbstractJooqDao
        implements RancherConfigToComposeFormatter {

    ObjectManager objManager;

    public RancherCertificatesToComposeFormatter(Configuration configuration, ObjectManager objManager) {
        super(configuration);
        this.objManager = objManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object format(ComposeExportConfigItem item, Object valueToTransform) {
        if (item.getDockerName().equalsIgnoreCase(ComposeExportConfigItem.CERTIFICATES.getDockerName())) {
            List<Number> certificateIds = (List<Number>) valueToTransform;
            List<String> certificateNames = new ArrayList<>();
            for (Number certificateId : certificateIds) {
                String certName = getCertName(certificateId);
                if (StringUtils.isNotBlank(certName)) {
                    certificateNames.add(certName);
                }
            }
            return certificateNames;
        } else if (item.getDockerName().equals(ComposeExportConfigItem.DEFAULT_CERTIFICATE.getDockerName())) {
            Number defaultCertId = (Number) valueToTransform;
            return getCertName(defaultCertId);
        } else {
            return null;
        }
    }

    private String getCertName(Number certId) {
        if (certId == null) {
            return null;
        }
        Certificate cert = objManager.loadResource(Certificate.class, certId.longValue());
        return cert == null ? null : cert.getName();
    }
}
