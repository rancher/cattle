package io.cattle.platform.liquibase;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Set;

import liquibase.servicelocator.DefaultPackageScanClassResolver;
import liquibase.servicelocator.PackageScanFilter;

import org.apache.commons.io.IOUtils;

public class JarInJarPackageScanner extends DefaultPackageScanClassResolver {

    public static final String JAR_IN_JAR_PREFIX = "jar:injar";

    @Override
    protected void find(PackageScanFilter test, String packageName, ClassLoader loader, Set<Class<?>> classes) {
        super.find(test, packageName, loader, classes);

        Enumeration<URL> urls;
        try {
            urls = getResources(loader, packageName);
        } catch (IOException ioe) {
            return;
        }

        InputStream is = null;
        while (urls.hasMoreElements()) {
            URL url = null;
            try {
                url = urls.nextElement();

                if (!isInJar(url))
                    continue;

                is = getInputStream(url);

                loadImplementationsInJar(test, packageName, is, url.toExternalForm(), classes);
            } catch (IOException e) {
            } finally {
                IOUtils.closeQuietly(is);
            }
        }
    }

    protected InputStream getInputStream(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (connection instanceof JarURLConnection) {
            url = ((JarURLConnection) connection).getJarFileURL();
        }
        URLConnection con = url.openConnection();
        return con.getInputStream();
    }

    protected boolean isInJar(URL url) {
        String urlPath = url.toExternalForm();

        return urlPath.startsWith(JAR_IN_JAR_PREFIX);
    }

}
