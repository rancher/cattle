package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
import static io.cattle.platform.core.model.tables.InstanceHostMapTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.SecretTable.*;

import io.cattle.platform.core.addon.SecretReference;
import io.cattle.platform.core.dao.SecretDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Secret;
import io.cattle.platform.core.model.tables.records.HostRecord;
import io.cattle.platform.core.model.tables.records.InstanceRecord;
import io.cattle.platform.core.model.tables.records.SecretRecord;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.jooq.Record;

@Named
public class SecretDaoImpl extends AbstractJooqDao implements SecretDao {

    @Override
    public InstanceAndHost getHostForInstanceUUIDAndAuthAccount(long accountId, String instanceUuid) {
        Record r = create().select(HOST.fields())
            .from(HOST)
            .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.HOST_ID.eq(HOST.ID))
            .join(INSTANCE)
                .on(INSTANCE.ID.eq(INSTANCE_HOST_MAP.INSTANCE_ID))
            .join(AGENT)
                .on(INSTANCE.AGENT_ID.eq(AGENT.ID))
            .where(AGENT.ACCOUNT_ID.eq(accountId)
                    .and(AGENT.REMOVED.isNull())
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
            .fetchAny();

        if (r == null) {
            return null;
        }

        Host host = r.into(HostRecord.class);

        r = create().select(INSTANCE.fields())
            .from(INSTANCE)
            .join(INSTANCE_HOST_MAP)
                .on(INSTANCE_HOST_MAP.INSTANCE_ID.eq(INSTANCE.ID))
            .where(INSTANCE_HOST_MAP.HOST_ID.eq(host.getId())
                    .and(INSTANCE.UUID.eq(instanceUuid))
                    .and(INSTANCE.REMOVED.isNull())
                    .and(INSTANCE_HOST_MAP.REMOVED.isNull()))
            .fetchAny();

        if (r == null) {
            return null;
        }

        return new InstanceAndHost(r.into(InstanceRecord.class), host);
    }

    @Override
    public Map<Long, Secret> getSecrets(List<SecretReference> refs) {
        Set<Long> ids = new HashSet<>();
        for (SecretReference ref : refs) {
            ids.add(ref.getSecretId());
        }

        Map<Long, Secret> result = new HashMap<Long, Secret>();

        for (Secret secret : create().select(SECRET.fields())
            .from(SECRET)
            .where(SECRET.ID.in(ids))
            .fetchInto(SecretRecord.class)) {
            result.put(secret.getId(), secret);
        }

        return result;
    }

}
