package io.cattle.platform.launcher.jetty;

import static io.cattle.platform.server.context.ServerContext.*;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
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

public class Main {

    public static final String WEB_XML = "WEB-INF/web.xml";
    public static final String OVERRIDE_WEB_XML = "WEB-INF/override-web.xml";
    public static final String STATIC_WEB_XML = "WEB-INF/static-override-web.xml";
    public static final String DEFAULT_WEB_XML = "WEB-INF/default-web.xml";

    // private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Logger CONSOLE_LOG = LoggerFactory.getLogger("ConsoleStatus");

    public static final String[] PREFIXES = new String[] { "code/packaging/app/src/main/webapp/", "src/main/webapp/", "" };

    protected static URL findUrl(String suffix) throws IOException {
        File file = findFile(suffix);
        if (file != null) {
            return file.toURI().toURL();
        }

        return Main.class.getResource("/" + suffix);
    }

    protected static File findFile(String suffix) {
        for (String prefix : PREFIXES) {
            File file = new File(prefix + suffix);

            if (file.exists())
                return new File(file.getAbsolutePath());
        }

        URL url = Main.class.getResource("/" + suffix);
        if (url != null && "file".equals(url.getProtocol())) {
            return new File(url.getPath());
        }

        return null;
    }

    protected static URL getContextRoot(URL webXml) throws IOException {
        if (webXml != null) {
            URLConnection connection = webXml.openConnection();
            if (connection instanceof JarURLConnection) {
                URL war = ((JarURLConnection) connection).getJarFileURL();
                return new URL("jar", "", war.toExternalForm() + "!/WEB-INF/content");
            }
        }
        return Main.class.getResource("");
    }

    protected static String getHttpPort() {
        boolean proxyEmbedded = StringUtils.equals(HOST_API_PROXY_MODE_EMBEDDED, getHostApiProxyMode());
        if(proxyEmbedded) {
            String port = System.getenv("CATTLE_HTTP_PROXIED_PORT");
            return port == null ? System.getProperty("cattle.http.proxied.port", "8081") : port;
        } else {
            String port = System.getenv("CATTLE_HTTP_PORT");
            return port == null ? System.getProperty("cattle.http.port", "8080") : port;
        }
    }

    public static void main(String... args) {
        /*
         * The world is better place without time zones. Well, at least for
         * computers
         */
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

            File webXmlFile = findFile(WEB_XML);

            URL webXml = findUrl(WEB_XML);
            URL contextRoot = webXmlFile == null ? getContextRoot(webXml) : webXmlFile.getParentFile().getParentFile().toURI().toURL();

            URL override = findUrl(OVERRIDE_WEB_XML);
            if (override != null) {
                context.setOverrideDescriptors(Arrays.asList(override.toExternalForm()));
            }

            URL defaultWebXml = findUrl(DEFAULT_WEB_XML);
            if (defaultWebXml != null) {
                context.setDefaultsDescriptor(defaultWebXml.toExternalForm());
            }

            URL staticOverideXml = findUrl(STATIC_WEB_XML);
            if (staticOverideXml != null && new File("./content").exists()) {
                List<String> overrides = new ArrayList<String>(context.getOverrideDescriptors());
                overrides.add(staticOverideXml.toExternalForm());
                context.setOverrideDescriptors(overrides);
            }

            if (contextRoot != null) {
                context.setWar(contextRoot.toExternalForm());
            }

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
}
