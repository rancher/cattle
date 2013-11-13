package org.apache.cloudstack.db.jooq.mapper;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

import com.cloud.utils.db.TransactionLegacy;

public class Configuration extends DefaultConfiguration {

    private static final long serialVersionUID = -726368732372005280L;

    @Inject
    CloudStackRecordMapperProvider recordMapperProvider;

    public Configuration() {
        super();
        set(SQLDialect.MYSQL);
        set(new AutoCommitConnectionProvider(TransactionLegacy.getDataSource(TransactionLegacy.CLOUD_DB)));
    }

    @PostConstruct
    public void init() {
        set(recordMapperProvider);
    }
}
