package io.cattle.platform.archaius.sources;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.sources.JDBCConfigurationSource;

public class LazyJDBCSource implements PolledConfigurationSource {

    private static final Logger log = LoggerFactory.getLogger(LazyJDBCSource.class);

    JDBCConfigurationSource source;
    boolean firstLoad = true;
    boolean firstDbError = true;

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        if (source == null) {
            return PollResult.createFull(new HashMap<String, Object>());
        }

        try {
            return source.poll(initial, checkPoint);
        } catch (SQLException e) {
            if (firstLoad) {
                return checkInitial(initial, checkPoint);
            }
            throw e;
        } finally {
            firstLoad = false;
        }
    }

    protected PollResult checkInitial(boolean initial, Object checkPoint) throws Exception {
        DataSource dataSource = source.getDatasource();
        Connection conn = null;
        boolean connectionGood = true;

        try {
            for (int i = 0; i < 300; i++) {
                try {
                    conn = dataSource.getConnection();
                    break;
                } catch (SQLException e) {
                    connectionGood = false;
                    if (firstDbError) {
                        firstDbError = false;
                        log.error("Failed to get connection to database, will retry for 5 minutes", e);
                    } else {
                        log.error("Failed to get connection to database, will retry for 5 minutes");
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException t) {
                        throw e;
                    }
                }
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }

        if (connectionGood) {
            /*
             * If connection was good, then we probably don't have the Settings
             * table yet, so just continue
             */
            return PollResult.createFull(new HashMap<String, Object>());
        } else {
            return source.poll(initial, checkPoint);
        }
    }

    public JDBCConfigurationSource getSource() {
        return source;
    }

    public void setSource(JDBCConfigurationSource source) {
        this.source = source;
    }

}
