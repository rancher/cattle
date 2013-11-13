package org.apache.cloudstack.db.jooq.mapper;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jooq.impl.DataSourceConnectionProvider;

public class AutoCommitConnectionProvider extends DataSourceConnectionProvider {

    public AutoCommitConnectionProvider(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Connection acquire() {
        Connection conn = super.acquire();
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to set auto commit", e);
        }
        return conn;
    }
}
