package io.cattle.platform.archaius.sources;

import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.PolledConfigurationSource;

import java.util.Iterator;

public class NamedDynamicConfiguration extends DynamicConfiguration implements NamedConfigurationSource {

    private String sourceName;

    public NamedDynamicConfiguration(PolledConfigurationSource source, AbstractPollingScheduler scheduler, String sourceName) {
        super(source, scheduler);
        this.sourceName = sourceName;
    }

    @Override
    public String getSourceName() {
        return sourceName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator<String> getKeys() {
        return (Iterator<String>) super.getKeys();
    }
}
