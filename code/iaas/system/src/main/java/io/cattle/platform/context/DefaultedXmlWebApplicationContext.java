package io.cattle.platform.context;

import org.springframework.web.context.support.XmlWebApplicationContext;

public class DefaultedXmlWebApplicationContext extends XmlWebApplicationContext {

    private static final String[] DEFAULT_LOCATIONS = new String[] { "classpath:META-INF/cattle/webApplicationContext.xml" };

    @Override
    protected String[] getDefaultConfigLocations() {
        return DEFAULT_LOCATIONS;
    }

}
