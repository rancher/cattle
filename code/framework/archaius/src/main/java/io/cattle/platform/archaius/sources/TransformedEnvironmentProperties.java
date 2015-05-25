package io.cattle.platform.archaius.sources;

public class TransformedEnvironmentProperties extends AbstractTransformedEnvironmentProperties {

    public TransformedEnvironmentProperties() {
        super("CATTLE", "CATTLE_");
    }

    @Override
    public String getSourceName() {
        return "Environment Variables";
    }

}
