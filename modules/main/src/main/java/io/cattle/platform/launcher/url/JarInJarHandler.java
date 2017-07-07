package io.cattle.platform.launcher.url;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class JarInJarHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        String x = u.getPath();
        x = x.replaceAll("___", ":");
        x = x.replaceAll("__", "!");
        return new URL(x).openConnection();
    }

    public static URL createJarInJar(URL jarUrl, String location) throws MalformedURLException {
        String preUrl = "jar:" + jarUrl.toExternalForm() + "!/" + location;
        preUrl = preUrl.replaceAll("!", "__");
        preUrl = preUrl.replaceAll(":", "___");

        return new URL(JarInJarHandlerFactory.INJAR_PROTOCOL + ":" + preUrl);
    }

}
