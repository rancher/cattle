package io.cattle.platform.agent.instance.service.impl;

import static io.cattle.platform.core.model.tables.HostIpAddressMapTable.*;

import io.cattle.platform.agent.instance.service.InstanceNicLookup;
import io.cattle.platform.core.model.HostIpAddressMap;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class IpAddressNicLookup extends AbstractJooqDao implements InstanceNicLookup {

    @Inject
    HostIpAddressMapNicLookup lookup;

    @Override
    public List<? extends Nic> getNics(Object obj) {
        if (!(obj instanceof IpAddress)) {
            return null;
        }

        List<Nic> nics = new ArrayList<>();
        IpAddress ip = (IpAddress)obj;

        List<? extends HostIpAddressMap> maps = create()
                .selectFrom(HOST_IP_ADDRESS_MAP)
                .where(HOST_IP_ADDRESS_MAP.IP_ADDRESS_ID.eq(ip.getId()))
                    .and(HOST_IP_ADDRESS_MAP.REMOVED.isNull())
                    .fetch();

        for (HostIpAddressMap map : maps) {
            List<? extends Nic> found = lookup.getNics(map);
            if (found != null) {
                nics.addAll(found);
            }
        }

        return nics;
    }

}