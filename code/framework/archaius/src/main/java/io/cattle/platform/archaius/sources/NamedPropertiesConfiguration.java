package io.cattle.platform.archaius.sources;

import java.io.File;
import java.net.URL;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

public class NamedPropertiesConfiguration extends PropertiesConfiguration implements NamedConfigurationSource {

    String sourceName;

    public NamedPropertiesConfiguration() {
        super();
    }

    public NamedPropertiesConfiguration(File file) throws ConfigurationException {
        super(file);
    }

    public NamedPropertiesConfiguration(String fileName) throws ConfigurationException {
        super(fileName);
    }

    public NamedPropertiesConfiguration(URL url) throws ConfigurationException {
        super(url);
    }

    @Override
    public String getSourceName() {
        return sourceName == null ? getFileName() : sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

}
