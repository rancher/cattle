package io.cattle.platform.db.jooq.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.jooq.ConnectionProvider;
import org.jooq.exception.DataAccessException;

public class AutoCommitConnectionProvider implements ConnectionProvider {

    private final DataSource dataSource;

    public AutoCommitConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection acquire() {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(true);
            return connection;
        } catch (SQLException e) {
            throw new DataAccessException("Error getting connection from data source " + dataSource, e);
        }
    }

    @Override
    public void release(Connection released) {
        try {
            released.close();
        } catch (SQLException e) {
            throw new DataAccessException("Error closing connection " + released, e);
        }
    }

}
