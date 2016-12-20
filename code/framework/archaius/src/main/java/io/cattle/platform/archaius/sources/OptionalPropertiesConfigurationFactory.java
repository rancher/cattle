package io.cattle.platform.archaius.sources;

import java.util.HashMap;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;

public class OptionalPropertiesConfigurationFactory {

    String name;

    public OptionalPropertiesConfigurationFactory() {
    }

    public OptionalPropertiesConfigurationFactory(String name) {
        this.name = name;
    }

    public AbstractConfiguration getConfiguration() {
        try {
            return new NamedPropertiesConfiguration(name);
        } catch (ConfigurationException e) {
            return new MapConfiguration(new HashMap<String, Object>());
        }
    }

    public static AbstractConfiguration getConfiguration(String name) {
        return new OptionalPropertiesConfigurationFactory(name).getConfiguration();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
