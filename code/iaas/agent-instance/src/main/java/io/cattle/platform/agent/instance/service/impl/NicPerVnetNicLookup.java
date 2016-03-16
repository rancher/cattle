package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.core.constants.NetworkConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class NicPerVnetNicLookup extends AbstractJooqDao {

    @Inject
    NetworkDao ntwkDao;

    @Inject
    ObjectManager objMgr;

    public List<? extends Nic> getRandomNicForAccount(long accountId) {
        List<Nic> nics = new ArrayList<>();
        List<? extends Network> ntwks = ntwkDao.getNetworksForAccount(accountId, NetworkConstants.KIND_HOSTONLY);
        if (ntwks.isEmpty()) {
            return nics;
        }
        Nic nic = create()
                .selectFrom(NIC)
                .where(NIC.NETWORK_ID.eq(ntwks.get(0).getId())
                        .and(NIC.REMOVED.isNull()))
                .fetchAny();
        if (nic != null) {
            nics.add(nic);
        }
        return nics;
    }
}
