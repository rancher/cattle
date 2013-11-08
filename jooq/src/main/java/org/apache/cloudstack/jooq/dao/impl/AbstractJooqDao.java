package org.apache.cloudstack.jooq.dao.impl;

import javax.inject.Inject;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.UpdateSetFirstStep;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

import com.cloud.utils.db.TransactionLegacy;

public class AbstractJooqDao {

    Configuration configuration;

    public AbstractJooqDao() {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(SQLDialect.MYSQL);
        configuration.set(new DataSourceConnectionProvider(TransactionLegacy.getDataSource(TransactionLegacy.CLOUD_DB)));

        this.configuration = configuration;
    }

    protected DSLContext create() {
        return new DefaultDSLContext(configuration);
    }

    protected SelectSelectStep<Record> select(Field<?>... fields) {
        return create().select();
    }

    protected <R extends Record> UpdateSetFirstStep<R> update(Table<R> table) {
        return create().update(table);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

}
