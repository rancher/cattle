package org.apache.cloudstack.launcher.url;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class JarInJarHandlerFactory implements URLStreamHandlerFactory {

	public static final String INJAR_PROTOCOL = "injar";
	
	JarInJarHandler handler = new JarInJarHandler();
	
	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if ( INJAR_PROTOCOL.equals(protocol) )
			return handler;
		return null;
	}

}
