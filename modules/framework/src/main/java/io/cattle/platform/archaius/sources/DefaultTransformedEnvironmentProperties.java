package io.cattle.platform.archaius.sources;

public class DefaultTransformedEnvironmentProperties extends AbstractTransformedEnvironmentProperties {

    public DefaultTransformedEnvironmentProperties() {
        super("", "DEFAULT_CATTLE_");
    }

    @Override
    public String getSourceName() {
        return "Default Environment Variables";
    }

}
