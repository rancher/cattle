package io.cattle.platform.jetty;

import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.TimeZone;

public class Main {

    public static final String WEB_XML = "web.xml";
    public static final String DEFAULT_WEB_XML = "default-web.xml";

    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    protected static String getHttpPort() {
        String port = System.getenv("CATTLE_HTTP_PORT");
        return port == null ? System.getProperty("cattle.http.port", "8081") : port;
    }

    private static void startJetty(String... args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        long start = System.currentTimeMillis();

        try {
            Server s = new Server();

            HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setRequestHeaderSize(16 * 1024);
            httpConfig.setOutputBufferSize(512);
            ServerConnector http = new ServerConnector(s, new HttpConnectionFactory(httpConfig));
            http.setPort(Integer.parseInt(getHttpPort()));
            s.setConnectors(new Connector[] {http});

            MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
            s.addEventListener(mbContainer);
            s.addBean(mbContainer);
            s.addBean(Log.getRootLogger());

            WebAppContext context = new WebAppContext();
            context.setThrowUnavailableOnStartupException(true);

            URL webXml = Main.class.getResource(WEB_XML);
            URL defaultWebXml = Main.class.getResource(DEFAULT_WEB_XML);

            context.setResourceBase(new File(".").getAbsolutePath());
            context.setDescriptor(webXml.toExternalForm());
            context.setDefaultsDescriptor(defaultWebXml.toExternalForm());
            context.setParentLoaderPriority(true);
            context.setClassLoader(new WebAppClassLoader(Main.class.getClassLoader(), context));
            context.setContextPath("/");

            s.setHandler(context);
            s.start();

            CONSOLE_LOG.info("[DONE ] [{}ms] Startup Succeeded, Listening on port {}", (System.currentTimeMillis() - start), getHttpPort());

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if ("--exit".equals(arg)) {
                    System.exit(0);
                }

                if ("--notify".equals(arg)) {
                    CONSOLE_LOG.info("[POST ] [{}ms] Calling notify [{}]", (System.currentTimeMillis() - start), args[i + 1]);
                    Runtime.getRuntime().exec(args[i + 1]).waitFor();
                }

            }

            s.join();
        } catch (Exception e) {
            e.printStackTrace();
            CONSOLE_LOG.error("Startup Failed [{}ms]", (System.currentTimeMillis() - start), e);
            System.err.println("STARTUP FAILED [" + (System.currentTimeMillis() - start) + "] ms");

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if ("--notify-error".equals(arg)) {
                    CONSOLE_LOG.error("[ERROR] [{}ms] Calling notify [{}]", (System.currentTimeMillis() - start), args[i + 1]);
                    try {
                        Runtime.getRuntime().exec(args[i + 1]).waitFor();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }

            System.exit(1);
        }
    }
    public static void main(String... args) {
        /* This main class basically just starts Jetty.  To see the real startup of Cattle refer to
         *      io.cattle.platform.app.Cattle
         */
        startJetty(args);
    }
}
