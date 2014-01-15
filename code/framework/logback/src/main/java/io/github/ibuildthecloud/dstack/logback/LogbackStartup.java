package io.github.ibuildthecloud.dstack.logback;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogbackStartup implements Runnable {

    private static final String LOGBACK_CONFIG = "logback/logback.xml";

    final static Logger log = LoggerFactory.getLogger(LogbackStartup.class);

    @Override
    public void run() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        URL url = LogbackStartup.class.getClassLoader().getResource(LOGBACK_CONFIG);
        if ( url != null ) {
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();

                configurator.doConfigure(url);
                log.info("Logback configured with [{}]", url);
            } catch (JoranException je) {
            }
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

}
