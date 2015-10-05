package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.HostTable.*;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.List;

public class HostDaoImpl extends AbstractJooqDao implements HostDao {

    @Override
    public List<? extends Host> getHosts(Long accountId, List<String> uuids) {
        return create()
            .selectFrom(HOST)
            .where(HOST.ACCOUNT_ID.eq(accountId)
            .and(HOST.STATE.notIn(CommonStatesConstants.REMOVED, CommonStatesConstants.REMOVING))
            .and(HOST.UUID.in(uuids))).fetch();
    }
}
