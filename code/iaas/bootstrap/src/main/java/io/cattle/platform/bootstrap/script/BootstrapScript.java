package io.cattle.platform.bootstrap.script;

import io.cattle.platform.archaius.util.ArchaiusUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import com.netflix.config.DynamicStringProperty;

public class BootstrapScript {

    private static final DynamicStringProperty BOOTSTRAP_SOURCE = ArchaiusUtil.getString("bootstrap.source");
    private static final DynamicStringProperty BOOTSTRAP_SOURCE_OVERRIDE = ArchaiusUtil.getString("bootstrap.source.override");
    private static final DynamicStringProperty REQUIRED_IMAGE = ArchaiusUtil.getString("bootstrap.required.image");

    public static byte[] getBootStrapSource() throws IOException {
        return getBootstrapSource(BOOTSTRAP_SOURCE_OVERRIDE.get(), BOOTSTRAP_SOURCE.get());
    }

    protected static byte[] getBootstrapSource(String... sources) throws IOException {
        ClassLoader cl = BootstrapScript.class.getClassLoader();
        for ( String source : sources ) {
            InputStream is = cl.getResourceAsStream(source);
            try {
                if ( is != null ) {
                    String content = IOUtils.toString(is);
                    content = content.replace("REQUIRED_IMAGE=", String.format("REQUIRED_IMAGE=\"%s\"", REQUIRED_IMAGE.get()));
                    return content.getBytes("UTF-8");
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        throw new FileNotFoundException("Failed to find [" + Arrays.toString(sources) + "]");
    }

}
