package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.HostTable.*;
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

import org.jooq.Configuration;
import org.jooq.Record;

public class SecretDaoImpl extends AbstractJooqDao implements SecretDao {

    public SecretDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public InstanceAndHost getHostForInstanceUUIDAndAuthAccount(long accountId, String instanceUuid) {
        Record r = create().select(HOST.fields())
            .from(HOST)
            .join(INSTANCE)
                .on(INSTANCE.HOST_ID.eq(HOST.ID))
            .join(AGENT)
                .on(INSTANCE.AGENT_ID.eq(AGENT.ID))
            .where(AGENT.ACCOUNT_ID.eq(accountId)
                    .and(AGENT.REMOVED.isNull())
                    .and(INSTANCE.REMOVED.isNull()))
            .fetchAny();

        if (r == null) {
            return null;
        }

        Host host = r.into(HostRecord.class);

        r = create().select(INSTANCE.fields())
            .from(INSTANCE)
            .where(INSTANCE.HOST_ID.eq(host.getId())
                    .and(INSTANCE.UUID.eq(instanceUuid))
                    .and(INSTANCE.REMOVED.isNull()))
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

        Map<Long, Secret> result = new HashMap<>();

        for (Secret secret : create().select(SECRET.fields())
            .from(SECRET)
            .where(SECRET.ID.in(ids))
            .fetchInto(SecretRecord.class)) {
            result.put(secret.getId(), secret);
        }

        return result;
    }

}
