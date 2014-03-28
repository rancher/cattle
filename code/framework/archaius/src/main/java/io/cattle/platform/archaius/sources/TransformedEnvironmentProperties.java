package io.cattle.platform.archaius.sources;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.StringUtils;

public class TransformedEnvironmentProperties extends MapConfiguration {

    private static final String CONTAINS = "CATTLE";
    private static final String PREFIX = "CATTLE_";

    public TransformedEnvironmentProperties() {
        super(getValues());
    }

    protected static Map<String,Object> getValues() {
        Map<String,Object> values = new HashMap<String, Object>();

        for ( Map.Entry<String, String> entry : System.getenv().entrySet() ) {
            String key = entry.getKey();
            if ( ! key.contains(CONTAINS) ) {
                continue;
            }

            if ( key.startsWith(PREFIX) ) {
                key = key.substring(PREFIX.length());
            }

            key = key.replace('_', '.').toLowerCase();

            if ( ! StringUtils.isBlank(entry.getValue()) ) {
                values.put(key, entry.getValue());
            }
        }

        return values;
    }
}
