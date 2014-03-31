package io.cattle.platform.archaius.sources;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.MapConfiguration;

public class NamedMapConfiguration extends MapConfiguration implements NamedConfigurationSource {

    String sourceName;

    public NamedMapConfiguration(Map<String, Object> map) {
        super(map);
    }

    public NamedMapConfiguration(Properties props) {
        super(props);
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

}
