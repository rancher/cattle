package io.cattle.iaas.healthcheck.service.impl;

import io.cattle.iaas.healthcheck.service.HealthcheckHostLookup;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class HostHealthcheckHostLookup extends AbstractJooqDao implements HealthcheckHostLookup {

    @Override
    public Host getHost(Object obj) {
        if (!(obj instanceof Host)) {
            return null;
        }
        return (Host) obj;
    }
}
