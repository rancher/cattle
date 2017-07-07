package io.cattle.platform.launcher.url;

import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class JarInJarHandlerFactory implements URLStreamHandlerFactory {

    public static final String INJAR_PROTOCOL = "injar";

    private boolean registered = false;
    JarInJarHandler handler = new JarInJarHandler();

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if (INJAR_PROTOCOL.equals(protocol))
            return handler;
        return null;
    }

    public synchronized void register() {
        if (!registered) {
            URL.setURLStreamHandlerFactory(this);
            registered = true;
        }
    }

}
