package io.cattle.platform.hazelcast.dao.impl;

import static io.cattle.platform.core.model.tables.DataTable.*;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;
import io.cattle.platform.hazelcast.dao.HazelcastDao;

import java.security.SecureRandom;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.jooq.exception.DataAccessException;

public class HazelcastDaoImpl extends AbstractJooqDao implements HazelcastDao {

    SecureRandom random = new SecureRandom();

    @Override
    public String getGroupName() {
        return getRandomString("hazelcast-group-name", true, true);
    }

    @Override
    public String getGroupPassword() {
        return getRandomString("hazelcast-group-password", false, false);
    }

    protected String getRandomString(String name, boolean visible, boolean uuid) {
        String value = get(name);

        if ( value == null ) {
            if ( uuid ) {
                value = UUID.randomUUID().toString();
            } else {
                byte[] bytes = new byte[64];
                random.nextBytes(bytes);
                value = Base64.encodeBase64String(bytes);
            }

            try {
                set(name, value, visible);
            } catch (DataAccessException e) {
                value = get(name);
                if ( value == null ) {
                    throw e;
                }
            }
        }

        return value;
    }

    protected String get(String name) {
        return create()
                .select(DATA.VALUE)
                .from(DATA)
                .where(DATA.NAME.eq(name))
                .fetchOne(DATA.VALUE);
    }

    protected void set(String name, String value, boolean visible) {
        create()
            .insertInto(DATA, DATA.NAME, DATA.VISIBLE, DATA.VALUE)
            .values(name, visible, value)
            .execute();
    }

}
