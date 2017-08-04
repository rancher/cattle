package io.cattle.platform.archaius.sources;

import org.apache.commons.configuration.Configuration;

public interface NamedConfigurationSource extends Configuration {

    String getSourceName();

}
