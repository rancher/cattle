package io.github.ibuildthecloud.dstack.archaius.sources;

import java.util.HashMap;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import com.netflix.config.sources.JDBCConfigurationSource;

public class LazyJDBCSource implements PolledConfigurationSource {

    JDBCConfigurationSource source;

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        if ( source == null ) {
            return PollResult.createFull(new HashMap<String, Object>());
        }

        return source.poll(initial, checkPoint);
    }

    public JDBCConfigurationSource getSource() {
        return source;
    }

    public void setSource(JDBCConfigurationSource source) {
        this.source = source;
    }

}
