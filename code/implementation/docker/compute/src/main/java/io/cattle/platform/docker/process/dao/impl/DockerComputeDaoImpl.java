package io.cattle.platform.docker.process.dao.impl;

import static io.cattle.platform.core.model.tables.IpAddressNicMapTable.*;
import static io.cattle.platform.core.model.tables.IpAddressTable.*;
import static io.cattle.platform.core.model.tables.NicTable.*;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.IpAddress;
import io.cattle.platform.core.model.tables.records.IpAddressRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.docker.constants.DockerIpAddressConstants;
import io.cattle.platform.docker.process.dao.DockerComputeDao;

import java.util.List;

public class DockerComputeDaoImpl extends AbstractJooqDao implements DockerComputeDao {

    @Override
    public IpAddress getDockerIp(String ip, Instance instance) {
        if ( instance == null || ip == null ) {
            return null;
        }

        List<IpAddressRecord> ips = create()
                .select(IP_ADDRESS.fields())
                .from(IP_ADDRESS)
                .join(IP_ADDRESS_NIC_MAP)
                    .on(IP_ADDRESS_NIC_MAP.IP_ADDRESS_ID.eq(IP_ADDRESS.ID))
                .join(NIC)
                    .on(IP_ADDRESS_NIC_MAP.NIC_ID.eq(NIC.ID))
                .where(NIC.INSTANCE_ID.eq(instance.getId())
                        .and(IP_ADDRESS.KIND.eq(DockerIpAddressConstants.KIND_DOCKER)
                                .or(IP_ADDRESS.ADDRESS.eq(ip))))
                .fetchInto(IpAddressRecord.class);

        if ( ips.size() == 0 ) {
            return null;
        } else if ( ips.size() > 1 ) {
            IpAddressRecord kindRecord = null;
            for ( IpAddressRecord record : ips ) {
                if ( record.getKind().equals(DockerIpAddressConstants.KIND_DOCKER) ) {
                    kindRecord = record;
                } else {
                    return record;
                }
            }

            return kindRecord;
        } else {
            return ips.get(0);
        }
    }

}
