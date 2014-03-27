package io.github.ibuildthecloud.dstack.agent.connection.ssh.dao.impl;

import static io.github.ibuildthecloud.dstack.core.model.tables.AccountTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.AgentTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.CredentialTable.*;
import static io.github.ibuildthecloud.dstack.core.model.tables.DataTable.*;
import io.github.ibuildthecloud.dstack.agent.connection.ssh.dao.SshAgentDao;
import io.github.ibuildthecloud.dstack.core.constants.CommonStatesConstants;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.db.jooq.dao.impl.AbstractJooqDao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jooq.Record2;
import org.jooq.RecordHandler;

public class SshAgentDaoImpl extends AbstractJooqDao implements SshAgentDao {

    private static final String PUBLIC_FORMAT = "ssh-client-key%s-public";
    private static final String PRIVATE_FORMAT = "ssh-client-key%s-private";
    private static final Pattern KEY_NAME_PATTERN = Pattern.compile("ssh-client-key([0-9]+)-(public|private)");

    @Override
    public String[] getLastestActiveApiKeys(Agent agent) {
        Record2<String, String> result = create()
                .select(CREDENTIAL.PUBLIC_VALUE, CREDENTIAL.SECRET_VALUE)
                .from(CREDENTIAL)
                .join(ACCOUNT)
                    .on(ACCOUNT.ID.eq(CREDENTIAL.ACCOUNT_ID))
                .join(AGENT)
                    .on(AGENT.ACCOUNT_ID.eq(ACCOUNT.ID))
                .where(
                        AGENT.ID.eq(agent.getId())
                        .and(CREDENTIAL.STATE.eq(CommonStatesConstants.ACTIVE)))
                .orderBy(CREDENTIAL.CREATED.desc())
                .fetchAny();

        return result == null ? null : new String[] { result.value1(), result.value2() };
    }

    @Override
    public List<String[]> getClientKeyPairs() {
        Map<Long,String[]> keys = readKeys();
        return new ArrayList<String[]>(keys.values());
    }

    protected Map<Long,String[]> readKeys() {
        final Map<Long,String[]> keys = new LinkedHashMap<Long, String[]>();

        create()
            .select(DATA.NAME, DATA.VALUE)
            .from(DATA)
            .where(
                    DATA.NAME.like("ssh-client-key%"))
            .orderBy(DATA.NAME.asc())
            .fetchInto(new RecordHandler<Record2<String,String>>() {
                @Override
                public void next(Record2<String,String> record) {
                    Matcher m = KEY_NAME_PATTERN.matcher(record.value1());
                    if ( ! m.matches() ) {
                        return;
                    }

                    Long key = new Long(m.group(1));
                    String type = m.group(2);

                    String[] value = keys.get(key);
                    if ( value == null ) {
                        value = new String[2];
                        keys.put(key, value);
                    }

                    int idx = type.equals("public") ? 0 : 1;
                    value[idx] = record.value2();
                }
           });

        return keys;
    }

    @Override
    public void saveKey(String publicPart, String privatePart) {
        Map<Long,String[]> keys = readKeys();

        long i = 1;
        while ( keys.containsKey(i) ) {
            i++;
        }

        create()
            .insertInto(DATA, DATA.NAME, DATA.VISIBLE, DATA.VALUE)
            .values(String.format(PUBLIC_FORMAT, i), true, publicPart)
            .values(String.format(PRIVATE_FORMAT, i), false, privatePart)
            .execute();
    }

}
