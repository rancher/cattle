package io.cattle.platform.configitem.freemarker;

import java.util.Properties;

/* The only purpose of this class is to narrow the return type of
 * getSettings() so that Spring can instantiate this on Java 8
 */
@SuppressWarnings("deprecation")
public class Configuration extends freemarker.template.Configuration {

    @SuppressWarnings("unchecked")
    @Override
    public Properties getSettings() {
        Properties props = new Properties();
        props.putAll(super.getSettings());
        return props;
    }

}
