package io.cattle.platform.logback;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

import com.netflix.config.DynamicStringProperty;

public class LogbackStartup implements Runnable {

    private static final DynamicStringProperty LOGBACK_CONFIG = ArchaiusUtil.getString("logback.config");

    final static Logger log = LoggerFactory.getLogger(LogbackStartup.class);

    @Override
    public void run() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        URL url = LogbackStartup.class.getClassLoader().getResource(LOGBACK_CONFIG.get());

        if (url != null) {
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(context);
                context.reset();

                configurator.doConfigure(url);
                log.info("Logback configured with [{}]", url);
            } catch (JoranException je) {
            }
        }

        // StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

}
