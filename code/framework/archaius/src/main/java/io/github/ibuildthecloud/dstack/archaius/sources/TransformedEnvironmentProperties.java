package io.github.ibuildthecloud.dstack.archaius.sources;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.MapConfiguration;

public class TransformedEnvironmentProperties extends MapConfiguration {

    private static final String PREFIX = "DSTACK_";

    public TransformedEnvironmentProperties() {
        super(getValues());
    }

    protected static Map<String,Object> getValues() {
        Map<String,Object> values = new HashMap<String, Object>();

        for ( Map.Entry<String, String> entry : System.getenv().entrySet() ) {
            String key = entry.getKey();
            if ( ! key.startsWith(PREFIX) ) {
                continue;
            }
            key = key.substring(PREFIX.length()).replace('_', '.').toLowerCase();
            values.put(key, entry.getValue());
        }

        return values;
    }
}
