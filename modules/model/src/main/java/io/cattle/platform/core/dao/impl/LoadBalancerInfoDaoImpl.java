package io.cattle.platform.core.dao.impl;

import static io.cattle.platform.core.model.tables.CertificateTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import static io.cattle.platform.core.model.tables.StackTable.*;

import io.cattle.platform.core.dao.LoadBalancerInfoDao;
import io.cattle.platform.db.jooq.dao.impl.AbstractJooqDao;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Configuration;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.RecordHandler;

public class LoadBalancerInfoDaoImpl extends AbstractJooqDao implements LoadBalancerInfoDao {

    public LoadBalancerInfoDaoImpl(Configuration configuration) {
        super(configuration);
    }

    @Override
    public Map<Long, Pair<String, String>> getServiceIdToServiceStackName(long accountId) {
        final Map<Long, Pair<String, String>> toReturn = new HashMap<>();
        create().select(SERVICE.ID, SERVICE.NAME, STACK.NAME)
                .from(SERVICE)
                .join(STACK)
                .on(STACK.ID.eq(SERVICE.STACK_ID))
                .where(SERVICE.ACCOUNT_ID.eq(accountId))
                .and(SERVICE.REMOVED.isNull())
                .fetchInto(new RecordHandler<Record3<Long, String, String>>() {
                    @Override
                    public void next(Record3<Long, String, String> record) {
                        toReturn.put(record.getValue(SERVICE.ID), Pair.of(record.getValue(SERVICE.NAME), record.getValue(STACK.NAME)));
                    }
                });
        return toReturn;
    }

    @Override
    public Map<Long, String> getInstanceIdToInstanceName(long accountId) {
        final Map<Long, String> toReturn = new HashMap<>();
        create().select(INSTANCE.ID, INSTANCE.NAME)
                .from(INSTANCE)
                .where(INSTANCE.ACCOUNT_ID.eq(accountId))
                .and(INSTANCE.REMOVED.isNull())
                .fetchInto(new RecordHandler<Record2<Long, String>>() {
                    @Override
                    public void next(Record2<Long, String> record) {
                        toReturn.put(record.getValue(INSTANCE.ID), record.getValue(INSTANCE.NAME));
                    }
                });
        return toReturn;
    }

    @Override
    public Map<Long, String> getCertificateIdToCertificate(long accountId) {
        final Map<Long, String> toReturn = new HashMap<>();
        create().select(CERTIFICATE.ID, CERTIFICATE.NAME)
                .from(CERTIFICATE)
                .where(CERTIFICATE.ACCOUNT_ID.eq(accountId))
                .and(CERTIFICATE.REMOVED.isNull())
                .fetchInto(new RecordHandler<Record2<Long, String>>() {
                    @Override
                    public void next(Record2<Long, String> record) {
                        toReturn.put(record.getValue(CERTIFICATE.ID), record.getValue(CERTIFICATE.NAME));
                    }
                });
        return toReturn;
    }

}
