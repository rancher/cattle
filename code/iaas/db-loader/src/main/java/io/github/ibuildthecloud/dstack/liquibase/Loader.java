package io.github.ibuildthecloud.dstack.liquibase;

import io.github.ibuildthecloud.dstack.core.model.tables.CoreChangelogTable;

import javax.inject.Inject;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;

import org.jooq.Configuration;
import org.jooq.impl.DSL;

public class Loader extends SpringLiquibase {

    Configuration configuration;

    @Override
    public void afterPropertiesSet() throws LiquibaseException {
        try {
            System.setProperty("liquibase.databaseChangeLogTableName", "core_changelog");
            System.setProperty("liquibase.databaseChangeLogLockTableName", "changelog_lock");
            //TODO acquire lock
            DSL.using(getConfiguration()).delete(CoreChangelogTable.CORE_CHANGELOG).execute();
        } catch ( Throwable t ) {
            //ignore all errors
        }
        super.afterPropertiesSet();
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

}
