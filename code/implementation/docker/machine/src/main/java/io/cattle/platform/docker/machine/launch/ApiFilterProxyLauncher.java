package io.cattle.platform.docker.machine.launch;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Credential;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.service.launcher.GenericServiceLauncher;
import io.cattle.platform.util.type.InitializationTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class ApiFilterProxyLauncher extends GenericServiceLauncher implements InitializationTask {

    private static final DynamicStringProperty API_FILTER_PROXY_BINARY = ArchaiusUtil.getString("api.filter.proxy.executable");
    private static final DynamicStringProperty API_FILTER_PROXY_CONFIG = ArchaiusUtil.getString("api.filter.proxy.config");
    private static final DynamicBooleanProperty API_FILTER_PROXY_LAUNCH = ArchaiusUtil.getBoolean("api.filter.proxy.execute");
    private static final DynamicStringProperty API_FILTER_PROXY_PORT = ArchaiusUtil.getString("api.filter.proxy.http.port");

    private static final Logger log = LoggerFactory.getLogger(ApiFilterProxyLauncher.class);

    public static class PreFilter {
        String name;
        String endpoint;
        String secretToken;
        String[] methods;
        String[] paths;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getSecretToken() {
            return secretToken;
        }

        public void setSecretToken(String secretToken) {
            this.secretToken = secretToken;
        }

        public String[] getPaths() {
            return paths;
        }

        public void setPaths(String[] paths) {
            this.paths = paths;
        }

        public String[] getMethods() {
            return methods;
        }

        public void setMethods(String[] methods) {
            this.methods = methods;
        }

        public PreFilter() {
        }
    }

    public static class Destination {
        String destinationURL;
        String[] paths;

        public String getDestinationURL() {
            return destinationURL;
        }

        public void setDestinationURL(String destinationURL) {
            this.destinationURL = destinationURL;
        }

        public String[] getPaths() {
            return paths;
        }

        public void setPaths(String[] paths) {
            this.paths = paths;
        }

        public Destination() {
        }
    }

    public static class ConfigFileFields {
        PreFilter[] preFilters;
        Destination[] destinations;

        public PreFilter[] getPrefilters() {
            return preFilters;
        }

        public void setPrefilters(PreFilter[] filters) {
            preFilters = filters;
        }

        public Destination[] getDestinations() {
            return destinations;
        }

        public void setDestinations(Destination[] destinations) {
            this.destinations = destinations;
        }

        public ConfigFileFields() {
        }
    }

    @Inject
    JsonMapper jsonMapper;

    @Override
    protected boolean shouldRun() {
        return API_FILTER_PROXY_LAUNCH.get();
    }

    @Override
    protected String binaryPath() {
        return API_FILTER_PROXY_BINARY.get();
    }

    @Override
    protected List<DynamicStringProperty> getReloadSettings() {
        List<DynamicStringProperty> list = new ArrayList<DynamicStringProperty>();
        list.add(API_FILTER_PROXY_CONFIG);
        return list;
    }

    @Override
    protected void prepareProcess(ProcessBuilder pb) throws IOException {
        List<String> args = pb.command();
        args.add("--config");
        prepareConfigFile();
        args.add("config.json");
        args.add("--listen");
        String listen = API_FILTER_PROXY_PORT.get();
        args.add(":" + listen);
        String cattleProxyAddress = "http://localhost:" + getCattleProxiedPort();
        args.add("--cattle-url");
        args.add(cattleProxyAddress);
    }

    protected void prepareConfigFile() throws IOException {
        File configFile = new File("config.json");

        String proxyConfig = API_FILTER_PROXY_CONFIG.get();
        ConfigFileFields configEntries = new ConfigFileFields();
        FileOutputStream fos;
        if(proxyConfig != null) {
            configEntries = jsonMapper.readValue(proxyConfig, ConfigFileFields.class);
            fos = new FileOutputStream(configFile.getAbsoluteFile());
            jsonMapper.writeValue(fos, configEntries);
        } else {
            //initialize with a default empty config
            fos = new FileOutputStream(configFile.getAbsoluteFile());
            jsonMapper.writeValue(fos, configEntries);
        }
    }

    @Override
    protected void setEnvironment(Map<String, String> env) {
        Credential cred = getCredential();
        env.put("CATTLE_ACCESS_KEY", cred.getPublicValue());
        env.put("CATTLE_SECRET_KEY", cred.getSecretValue());
        String cattleProxyAddress = "http://localhost:" + getCattleProxiedPort();
        env.put("CATTLE_URL", cattleProxyAddress);
    }


    private String getCattleProxiedPort() {
        //it is assumed that when api.filter.proxy.execute=true, WSP is always ON and cattle is listening on proxied-port/8081. 
        //The case where WSP is OFF but filter-proxy is ON is not valid.
        String port = System.getenv("CATTLE_HTTP_PROXIED_PORT");
        return port == null ? System.getProperty("cattle.http.proxied.port", "8081") : port;
    }

    @Override
    protected LockDefinition getLock() {
        return null;
    }

    @Override
    protected boolean isReady() {
        return true;
    }

    @Override
    public void reload() {
        if (!shouldRun()) {
            return;
        }

        try {
            prepareConfigFile();
            StringBuilder apiProxyUrl = new StringBuilder();
            apiProxyUrl.append("http://localhost:").append(API_FILTER_PROXY_PORT.get()).append("/v1-api-filter-proxy/reload");
            Request.Post(apiProxyUrl.toString()).execute();
        } catch (IOException e) {
            log.info("Failed to reload api proxy service: {}", e.getMessage());
        }
    }

}
