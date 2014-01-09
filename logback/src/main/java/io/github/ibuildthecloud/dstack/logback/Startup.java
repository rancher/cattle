package io.github.ibuildthecloud.dstack.logback;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup {

    private static final String LOGBACK_DISABLE = "dstack.logback.disable";
    private static final String LOGBACK_SYSTEM_CONFIG = "logback.configurationFile";
    private static final String LOGBACK_CLASS = "ch.qos.logback.classic.LoggerContext";
    private static final String[] STANDARD_CONFIG = new String[] {
        "logback.xml",
        "logback.groovy"
    };

    final static Logger log = LoggerFactory.getLogger(Startup.class);

    @PostConstruct
    public void init() {
        if ( shouldConfigure() ) {
            try {
                Class<?> clz = Class.forName("io.github.ibuildthecloud.dstack.logback.LogbackStartup");
                ((Runnable)clz.newInstance()).run();
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
        if ( System.getProperty(LOGBACK_SYSTEM_CONFIG) != null ) {
            return false;
        }

        /* There is a logback config file on the classpath */
        for ( String config : STANDARD_CONFIG ) {
            if ( this.getClass().getClassLoader().getResource(config) != null ) {
                return false;
            }
        }

        /* Hook to disable logback configuration for whatever reason */
        if ( Boolean.getBoolean(LOGBACK_DISABLE) ) {
            return false;
        }

        return true;
    }
}
