package io.cattle.platform.archaius.sources;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.MapConfiguration;

import java.io.File;
import java.util.HashMap;

public class OptionalPropertiesConfigurationFactory {

    String name;
    File file;

    public OptionalPropertiesConfigurationFactory(String name) {
        this.name = name;
    }

    public OptionalPropertiesConfigurationFactory(File file) {
        this.file = file;
    }

    public AbstractConfiguration getConfiguration() {
        try {
            if (file == null) {
                return new NamedPropertiesConfiguration(name);
            } else {
                return new NamedPropertiesConfiguration(file);
            }
        } catch (ConfigurationException e) {
            return new MapConfiguration(new HashMap<>());
        }
    }

    public static AbstractConfiguration getConfiguration(String name) {
        return new OptionalPropertiesConfigurationFactory(name).getConfiguration();
    }

    public static AbstractConfiguration getConfiguration(File f) {
        return new OptionalPropertiesConfigurationFactory(f).getConfiguration();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
