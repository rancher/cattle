package io.cattle.platform.archaius.sources;

import static org.junit.Assert.*;

import java.util.Properties;

import org.apache.commons.configuration.MapConfiguration;
import org.junit.Test;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationBackedDynamicPropertySupportImpl;

public class NamedPropertiesConfigurationTest {

    @Test
    public void test() {
        Properties props = new Properties();
        props.setProperty("a", "${b} ${d}");
        props.setProperty("b", "4 5 6 ${c}");
        props.setProperty("c", "1 2 3");
        props.setProperty("d", "7 8 9");

        MapConfiguration config = new MapConfiguration(props);

        assertEquals("4 5 6 1 2 3 7 8 9", config.getString("a"));
    }

    @Test
    public void testList() {
        Properties props = new Properties();
        props.setProperty("a", "${b.1},${d}");
        props.setProperty("b.1", "4,5,6,${c}");
        props.setProperty("c", "1,2,3");
        props.setProperty("d", "7,8,9");

        MapConfiguration mapConfig = new MapConfiguration(props);
        mapConfig.setDelimiterParsingDisabled(true);

        ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();
        config.addConfiguration(mapConfig);
        config.addConfiguration(new MapConfiguration(new Properties()));

        assertEquals("4,5,6,1,2,3,7,8,9", config.getString("a"));

        ConfigurationBackedDynamicPropertySupportImpl impl = new ConfigurationBackedDynamicPropertySupportImpl(config);

        assertEquals("4,5,6,1,2,3,7,8,9", impl.getString("a"));
    }

}
