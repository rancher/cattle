package io.cattle.platform.packaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Pack200;
import java.util.jar.Pack200.Packer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class Compress {

    private static String UUID = System.getProperty("uuid", java.util.UUID.randomUUID().toString());
    private static String[] EXCLUDES = new String[] {
        "mysql-connector-java",
        "mariadb-java-client"
    };

    private static class NoCloseInputStream extends FilterInputStream {
        protected NoCloseInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
        }
    }

    protected static boolean containsSha1(Manifest m) {
        if ( m == null ) {
            return false;
        }

        for ( Attributes attr : m.getEntries().values() ) {
            for ( Object i : attr.values() ) {
                i.toString().startsWith("SHA1");
                return true;
            }
        }

        return false;
    }

    public static final void compressJar(InputStream is, JarEntry entry, String output) throws IOException {
        JarInputStream jis = new JarInputStream(new NoCloseInputStream(is), true);
        if ( containsSha1(jis.getManifest()) ) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            System.out.println("Stripping signature on " + entry.getName());
            JarOutputStream jos = new JarOutputStream(baos);
            jos.setLevel(0);
            try {
                JarEntry jarEntry = null;
                while ( ( jarEntry = jis.getNextJarEntry() ) != null ) {
                    jos.putNextEntry(new JarEntry(jarEntry.getName()));
                    IOUtils.copy(jis, jos);
                    jos.closeEntry();
                }
            } finally {
                IOUtils.closeQuietly(jos);
            }

            jis = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()));
        }

        File outputFile = new File(output, entry.getName() + ".pack");
        FileUtils.forceMkdir(outputFile.getParentFile());

        System.out.println("Compressing " + entry.getName() + " to " + outputFile);
        FileOutputStream fos = new FileOutputStream(outputFile);
        try {
            Packer packer = Pack200.newPacker();
            packer.properties().put(Packer.DEFLATE_HINT, Packer.FALSE);
            packer.properties().put(Packer.EFFORT, "9");
            packer.pack(jis, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    public static final void compress(String input, String output) throws IOException {
        FileUtils.forceMkdir(new File(output));

        FileOutputStream fis = new FileOutputStream(new File(output, "id"));
        try {
            fis.write(UUID.getBytes("UTF-8"));
        } finally {
            IOUtils.closeQuietly(fis);
        }

        File outputDir = new File(output);
        if ( ! outputDir.exists() && ! outputDir.mkdirs() ) {
            throw new IOException("Failed to create directory [" + outputDir.getAbsolutePath() + "]");
        }

        File resourcesFile = new File(output, "resources.jar");
        JarInputStream is = new JarInputStream(new FileInputStream(input));
        Manifest m = new Manifest(is.getManifest());
        m.getMainAttributes().putValue("X-cattle-id", UUID);

        JarOutputStream resources = new JarOutputStream(new FileOutputStream(new File(output, "resources.jar")), m);

        try {
            JarEntry entry = null;
            while ( ( entry = is.getNextJarEntry() ) != null ) {
                String name = entry.getName();
                if ( ! exclude(name) && name.endsWith(".jar") ) {
                    compressJar(is, entry, output);
                } else {
                    System.out.println("Adding [" + entry.getName() + "] to [" + resourcesFile.getPath() + "]");
                    resources.putNextEntry(entry);
                    IOUtils.copy(is, resources);
                    resources.closeEntry();
                }
            }
        } finally {
            IOUtils.closeQuietly(resources);
        }
    }

    protected static boolean exclude(String name) {
        for ( String exclude : EXCLUDES ) {
            if ( name.contains(exclude) ) {
                return true;
            }
        }

        return false;
    }

    public static final void main(String... args) {
        try {
            compress(args[0], args[1]);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
