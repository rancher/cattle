package io.cattle.platform.db.jooq.dao.impl;

import javax.inject.Inject;

import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.UpdateSetFirstStep;
import org.jooq.impl.DefaultDSLContext;

public class AbstractJooqDao {

    Configuration configuration;

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
