package io.cattle.platform.bootstrap.script;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

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

    public static byte[] getBootStrapSource(ApiRequest apiRequest) throws IOException {
        return getBootstrapSource(apiRequest, BOOTSTRAP_SOURCE_OVERRIDE.get(), BOOTSTRAP_SOURCE.get());
    }

    protected static byte[] getBootstrapSource(ApiRequest apiRequest, String... sources) throws IOException {
        ClassLoader cl = BootstrapScript.class.getClassLoader();
        for (String source : sources) {
            InputStream is = cl.getResourceAsStream(source);
            try {
                if (is != null) {
                    String content = IOUtils.toString(is);
                    content = content.replace("REQUIRED_IMAGE=", String.format("REQUIRED_IMAGE=\"%s\"", REQUIRED_IMAGE.get()));
                    content = content.replace("DETECTED_CATTLE_AGENT_IP=", String.format("DETECTED_CATTLE_AGENT_IP=\"%s\"", apiRequest.getClientIp()));
                    return content.getBytes("UTF-8");
                }
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        throw new FileNotFoundException("Failed to find [" + Arrays.toString(sources) + "]");
    }

}
