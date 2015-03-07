package io.cattle.platform.launcher;

import io.cattle.platform.launcher.url.JarInJarHandler;
import io.cattle.platform.launcher.url.JarInJarHandlerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class Main {

    public static final String LIB_PREFIX = "WEB-INF/lib";
    public static final String JETTY_PREFIX = "WEB-INF/jetty";
    public static final String[] URL_PATHS = new String[] { JETTY_PREFIX, LIB_PREFIX };
    public static final String JETTY_LAUNCHER = "io.cattle.platform.launcher.jetty.Main";

    JarInJarHandlerFactory factory = new JarInJarHandlerFactory();

    protected URL getThisLocation() {
        ProtectionDomain domain = Main.class.getProtectionDomain();
        CodeSource source = domain.getCodeSource();
        return source.getLocation();
    }

    protected void runMain(ClassLoader cl, String... args) throws Exception {
        Thread.currentThread().setContextClassLoader(cl);

        Class<?> mainClass = cl.loadClass(JETTY_LAUNCHER);
        Method mainMethod = mainClass.getMethod("main", String[].class);

        mainMethod.invoke(null, (Object) args);
    }

    protected boolean isJar(URL url) {
        JarFile jarFile = null;
        try {
            File file = new File(url.getPath());
            jarFile = new JarFile(file);
            jarFile.getManifest();
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
            }
        }
    }

    protected ClassLoader getClassLoader() throws Exception {
        URL thisLocation = getThisLocation();

        List<URL> urls = Collections.emptyList();

        if (isJar(thisLocation)) {
            urls = collectJarUrls(thisLocation);
        } else {
            urls = collectDirectoryUrls(thisLocation);
        }

        if (urls.size() == 0)
            return this.getClass().getClassLoader();

        urls.addAll(getPlugins());
        urls.add(0, thisLocation);

        URL[] urlArray = urls.toArray(new URL[urls.size()]);

        ClassLoader cl = Boolean.getBoolean("cattle.main.inherit.cl") ? Main.class.getClassLoader() : null;
        return new URLClassLoader(urlArray, cl, factory);
    }

    protected List<URL> collectDirectoryUrls(URL url) throws IOException {
        if (!url.getProtocol().equals("file")) {
            return Collections.emptyList();
        }

        File dir = new File(url.getPath());
        List<URL> urls = new ArrayList<URL>();

        for (String name : URL_PATHS) {
            File path = new File(dir, name);
            if (path.exists()) {
                File[] children = path.listFiles();

                if (children != null) {
                    for (File child : path.listFiles()) {
                        if (!child.isDirectory() && child.getName().endsWith(".jar")) {
                            urls.add(child.toURI().toURL());
                        }
                    }
                }
            }
        }

        return urls;
    }

    protected List<URL> collectJarUrls(URL jarUrl) throws IOException {
        List<URL> jarsInJar = new ArrayList<URL>();

        InputStream is = null;
        JarInputStream jis = null;

        try {
            is = jarUrl.openStream();
            jis = new JarInputStream(is);

            for (JarEntry e = jis.getNextJarEntry(); e != null; e = jis.getNextJarEntry()) {
                String name = e.getName();

                if (!name.endsWith(".jar"))
                    continue;

                if (name.startsWith(LIB_PREFIX) || name.startsWith(JETTY_PREFIX)) {
                    factory.register();
                    jarsInJar.add(JarInJarHandler.createJarInJar(jarUrl, name));
                }
            }
        } finally {
            if (jis != null) {
                jis.close();
            }

            if (is != null) {
                is.close();
            }
        }

        return jarsInJar;
    }

    protected String getMain() {
        return System.getProperty("main", JETTY_LAUNCHER);
    }

    protected void setupHome() {
        String homeEnv = System.getenv("CATTLE_HOME");
        String homeProperty = System.getProperty("cattle.home");
        String home = homeProperty;

        if (home == null) {
            home = homeEnv;
        }

        if (home != null && !home.endsWith(File.separator)) {
            home += File.separator;
        }

        if (home != null && !home.equals(homeProperty)) {
            System.setProperty("cattle.home", home);
        }
    }

    public void run(String... args) throws Exception {
        /*
         * The world is better place without time zones. Well, at least for
         * computers
         */
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        setupHome();

        try {
            ClassLoader cl = getClassLoader();

            Thread.currentThread().setContextClassLoader(cl);

            Class<?> mainClass = cl.loadClass(getMain());
            Method mainMethod = mainClass.getMethod("main", String[].class);

            mainMethod.invoke(null, (Object) args);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected List<URL> getPlugins() throws IOException {
        String base = System.getenv("CATTLE_HOME");
        if (base == null) {
            base = System.getProperty("cattle.home", ".");
        }
        String pathStrings = System.getenv("CATTLE_EXTENSIONS");
        if (pathStrings == null) {
            pathStrings = System.getProperty("cattle.extensions", "etc/cattle,extensions");
        }

        String[] paths = pathStrings.trim().split("\\s*[,;:]\\s*");

        final List<URL> result = new ArrayList<URL>();

        for (String path : paths) {
            if (path.length() == 0)
                continue;

            File file = new File(base, path);

            if (file.exists()) {
                result.add(file.toURI().toURL());
                System.out.println("[MAIN] Scanning [" + path + "] for extensions");
                traverse(file.getAbsolutePath(), result);
            }
        }

        return result;
    }

    protected void traverse(String path, List<URL> result) {
        File file = new File(path);

        if (!file.exists()) {
            System.err.println("[MAIN] Failed to find : " + path);
            return;
        }

        for (File testFile : file.listFiles()) {
            if (testFile.isDirectory()) {
                traverse(testFile.getAbsolutePath(), result);
            } else if (testFile.getName().endsWith(".jar")) {
                try {
                    URL plugin = testFile.toURI().toURL();
                    System.out.println("[MAIN] Plugin : " + plugin);
                    result.add(plugin);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String... args) {
        try {
            new Main().run(args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
