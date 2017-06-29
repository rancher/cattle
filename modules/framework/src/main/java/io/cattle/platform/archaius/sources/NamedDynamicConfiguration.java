package io.cattle.platform.archaius.sources;

import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.PolledConfigurationSource;

public class NamedDynamicConfiguration extends DynamicConfiguration implements NamedConfigurationSource {

    String sourceName;

    public NamedDynamicConfiguration() {
        super();
    }

    public NamedDynamicConfiguration(PolledConfigurationSource source, AbstractPollingScheduler scheduler, String sourceName) {
        super(source, scheduler);
        this.sourceName = sourceName;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

}
