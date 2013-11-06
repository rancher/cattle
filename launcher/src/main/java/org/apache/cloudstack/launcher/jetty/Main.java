package org.apache.cloudstack.launcher.jetty;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.TimeZone;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;


public class Main {

	public static final String WEB_XML = "/WEB-INF/web.xml";

	public static final String[] WEB_XML_PATHS = new String[] {
			"app/src/main/webapp/WEB-INF/web.xml",
			"src/main/webapp/WEB-INF/web.xml" };

	protected static File getWebXml() {
		for (String webXmlPath : WEB_XML_PATHS) {
			File webXml = new File(webXmlPath);

			if (webXml.exists())
				return webXml;
		}

		return null;
	}
	
	protected static URL getContextRoot(URL webXml) throws IOException {
	    if ( webXml != null ) {
    	    URLConnection connection = webXml.openConnection();
            if ( connection instanceof JarURLConnection ) {
                URL war = ((JarURLConnection)connection).getJarFileURL();
                return new URL("jar", "", war.toExternalForm() + "!/WEB-INF/content");
            }
	    }
        return Main.class.getResource("");
	}

	public static void main(String... args) {
		/* The world is better place without time zones.  Well, at least for computers */
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		long start = System.currentTimeMillis();

		try {
			Server s = new Server(8080);

			WebAppContext context = new WebAppContext();
			context.setThrowUnavailableOnStartupException(true);

			File webXmlFile = getWebXml();

			URL webXml = webXmlFile == null ? Main.class.getResource(WEB_XML) : webXmlFile.toURI().toURL();
			URL contextRoot = webXmlFile == null ? getContextRoot(webXml) :
				webXmlFile.getParentFile().getParentFile().toURI().toURL();

			if ( webXml != null ) {
				context.setDescriptor(webXml.toExternalForm());
			}

			if ( contextRoot != null ) {
				context.setWar(contextRoot.toExternalForm());
			}

			context.setClassLoader(new WebAppClassLoader(Main.class.getClassLoader(), context));
			context.setContextPath("/client");

			s.setHandler(context);

			s.start();
			s.join();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("STARTUP FAILED [" + (System.currentTimeMillis() - start) + "] ms");
			System.exit(1);
		}
	}
}
