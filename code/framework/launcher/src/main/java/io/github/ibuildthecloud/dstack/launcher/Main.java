package io.github.ibuildthecloud.dstack.launcher;

import io.github.ibuildthecloud.dstack.launcher.url.JarInJarHandler;
import io.github.ibuildthecloud.dstack.launcher.url.JarInJarHandlerFactory;

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
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public class Main {

//	public static final String[] WEB_XML_PATHS = new String[] {
//		"app/src/main/webapp/WEB-INF/web.xml",
//		"src/main/webapp/WEB-INF/web.xml"
//	};
//	
	public static final String LIB_PREFIX = "WEB-INF/lib";
	public static final String JETTY_PREFIX = "WEB-INF/jetty";
	public static final String JETTY_LAUNCHER = "io.github.ibuildthecloud.dstack.launcher.jetty.Main";
	
	JarInJarHandlerFactory factory = new JarInJarHandlerFactory();
	
//	protected File getWebXml() {
//		for ( String webXmlPath : WEB_XML_PATHS ) {
//			File webXml = new File(webXmlPath);
//			
//			if ( webXml.exists() )
//				return webXml;
//		}
//		
//		return null;
//	}
	
	protected URL getThisLocation() {
		ProtectionDomain domain = Main.class.getProtectionDomain();
		CodeSource source = domain.getCodeSource();
		return source.getLocation();
	}
	
	protected void runMain(ClassLoader cl, String... args) throws Exception {
		Thread.currentThread().setContextClassLoader(cl);
		
		Class<?> mainClass = cl.loadClass(JETTY_LAUNCHER);
		Method mainMethod = mainClass.getMethod("main", String[].class);
		
		mainMethod.invoke(null, (Object)args);
	}
	
	protected boolean isJar(URL url) {
	    JarFile jarFile = null;
	    try {
    	    File file = new File(url.getPath());
    	    jarFile = new JarFile(file);
    	    jarFile.getManifest();
    	    return true;
	    } catch ( IOException e ) {
	        return false;
	    } finally {
	        if ( jarFile != null ) {
	            try {
                    jarFile.close();
                } catch (IOException e) {
                }
	        }
	    }
	}
	
	protected ClassLoader getClassLoader() throws Exception {
		URL thisLocation = getThisLocation();
		
		if ( ! isJar(thisLocation) ) {
			return this.getClass().getClassLoader();
		}
		
		List<URL> urls = collectionUrls(thisLocation);
		
		if ( urls.size() == 0 )
			return this.getClass().getClassLoader();

		urls.addAll(getPlugins());
		urls.add(0, thisLocation);
		
		URL[] urlArray = urls.toArray(new URL[urls.size()]);

		return new URLClassLoader(urlArray, null, factory);
	}
	

	protected List<URL> collectionUrls(URL jarUrl) throws IOException {
		List<URL> jarsInJar = new ArrayList<URL>();

		InputStream is = null;
		JarInputStream jis = null;
		
		try {
			is = jarUrl.openStream();
			jis = new JarInputStream(is);
			
			for ( JarEntry e = jis.getNextJarEntry() ; e != null ; e = jis.getNextJarEntry() ) {
				String name = e.getName();
				
				if ( ! name.endsWith(".jar") )
					continue;
				
				if ( name.startsWith(LIB_PREFIX) || name.startsWith(JETTY_PREFIX) ) {
				    factory.register();
					jarsInJar.add(JarInJarHandler.createJarInJar(jarUrl, name));
				}
			}
		} finally {
			if ( jis != null ) {
				jis.close();
			}
			
			if ( is != null ) {
				is.close();
			}
		}
		
		return jarsInJar;
	}

	public void run(String... args) throws Exception {
		try {
			ClassLoader cl = getClassLoader();
			
			Thread.currentThread().setContextClassLoader(cl);
			
			Class<?> mainClass = cl.loadClass(JETTY_LAUNCHER);
			Method mainMethod = mainClass.getMethod("main", String[].class);
			
			mainMethod.invoke(null, (Object)args);
					
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	protected List<URL> getPlugins() {
		String[] paths = System.getProperty("dstack.plugins", "plugins").trim().split("\\s*:\\s*");
		
		final List<URL> result = new ArrayList<URL>();
		
		for ( String path : paths ) {
			if ( path.length() == 0 )
				continue;
			
			File file = new File(path);

	        if ( file.exists() ) {
	            System.out.println("Scanning [" + path + "] for plugins");
	            traverse(path, result);
	        }
		}
		
		return result;
	}

	protected void traverse(String path, List<URL> result) {
		File file = new File(path);
		
		if ( ! file.exists() ) {
			System.err.println("Failed to find : " + path);
			return;
		}

		for ( File testFile : file.listFiles() ) {
			if ( testFile.isDirectory() ) {
				traverse(testFile.getAbsolutePath(), result);
			} else if ( testFile.getName().endsWith(".jar") ) {
				try {
					URL plugin = testFile.toURI().toURL();
					System.out.println("Plugin : " + plugin);
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
