package io.cattle.platform.logback;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup {

    private static final String DEFAULT_TOKEN = "CATTLE_DEFAULT_LOGBACK_XML";
    private static final String LOGBACK_DISABLE = "cattle.logback.disable";
    private static final String LOGBACK_SYSTEM_CONFIG = "logback.configurationFile";
    private static final String LOGBACK_CLASS = "ch.qos.logback.classic.LoggerContext";
    private static final String LOGBACK_XML = "logback.xml";
    private static final String[] STANDARD_CONFIG = new String[] { "logback.groovy", "logback-test.xml" };

    final static Logger log = LoggerFactory.getLogger(Startup.class);

    @PostConstruct
    public void init() {
        if (shouldConfigure()) {
            try {
                Class<?> clz = Class.forName("io.cattle.platform.logback.LogbackStartup");
                ((Runnable) clz.newInstance()).run();
            } catch (Exception e) {
                log.warn("Failed to configure logback : {}", e.getMessage());
                log.info("Failed to configure logback", e);
            }
        }
    }

    protected boolean shouldConfigure() {
        /* If there is no logback, probably shouldn't configure it */
        try {
            Class.forName(LOGBACK_CLASS);
        } catch (ClassNotFoundException e) {
            return false;
        }

        /* Standard logback config file configuration */
        if (System.getProperty(LOGBACK_SYSTEM_CONFIG) != null) {
            return false;
        }

        /* Look for a logback.xml file that is not ours */
        URL url = Startup.class.getClassLoader().getResource(LOGBACK_XML);
        InputStream is = null;
        try {
            is = url.openStream();
            if (!IOUtils.toString(is).contains(DEFAULT_TOKEN)) {
                return false;
            }
        } catch (IOException e) {
            log.error("Failed to read logback config", e);
        } finally {
            IOUtils.closeQuietly(is);
        }

        /* Look for other standard logback config files on the classpath */
        for (String config : STANDARD_CONFIG) {
            if (this.getClass().getClassLoader().getResource(config) != null) {
                return false;
            }
        }

        /* Hook to disable logback configuration for whatever reason */
        if (Boolean.getBoolean(LOGBACK_DISABLE)) {
            return false;
        }

        return true;
    }
}
