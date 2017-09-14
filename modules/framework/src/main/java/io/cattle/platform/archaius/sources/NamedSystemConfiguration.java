package io.cattle.platform.archaius.sources;

import org.apache.commons.configuration.SystemConfiguration;

public class NamedSystemConfiguration extends SystemConfiguration implements NamedConfigurationSource {

    @Override
    public String getSourceName() {
        return "Java system properties";
    }

}
