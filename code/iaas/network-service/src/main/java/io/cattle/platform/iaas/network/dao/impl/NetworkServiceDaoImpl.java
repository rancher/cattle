package io.cattle.platform.iaas.network.dao.impl;

import static io.cattle.platform.core.model.tables.NetworkServiceTable.*;

import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.iaas.network.dao.NetworkServiceDao;

public class NetworkServiceDaoImpl extends AbstractJooqDao implements NetworkServiceDao {

    @Override
    public NetworkService getService(Long networkId, String networkService) {
        if ( networkId == null ) {
            return null;
        }

        return create()
                .selectFrom(NETWORK_SERVICE)
                .where(NETWORK_SERVICE.NETWORK_ID.eq(networkId)
                        .and(NETWORK_SERVICE.KIND.eq(networkService))
                        .and(NETWORK_SERVICE.REMOVED.isNull()))
                .fetchAny();
    }

}
