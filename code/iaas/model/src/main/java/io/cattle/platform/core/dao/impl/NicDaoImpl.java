package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.NicTable.*;

import io.cattle.platform.core.dao.NicDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

public class NicDaoImpl extends AbstractJooqDao implements NicDao {

    @Override
    public Nic getPrimaryNic(Instance instance) {
        if ( instance == null ) {
            return null;
        }

        return create()
                .selectFrom(NIC)
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(NIC.DEVICE_NUMBER.eq(0)))
                .fetchOne();
    }

}
