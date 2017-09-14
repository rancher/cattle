package io.cattle.platform.api.register;

import com.netflix.config.DynamicStringProperty;
import io.cattle.platform.api.cluster.ClusterOutputFilter;
import io.cattle.platform.api.requesthandler.ScriptsHandler;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.CredentialConstants;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.register.auth.RegistrationAuthTokenManager;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.cattle.platform.util.resource.UUID;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RegisterScriptHandler implements ScriptsHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterScriptHandler.class);

    private static final DynamicStringProperty IMAGE = ArchaiusUtil.getString("bootstrap.required.image");
    private static final DynamicStringProperty SCRIPT = ArchaiusUtil.getString("agent.instance.register.script");
    private static final DynamicStringProperty YAML = ArchaiusUtil.getString("agent.instance.register.yaml");
    private static final DynamicStringProperty URL = ArchaiusUtil.getString("agent.instance.register.url");

    RegistrationAuthTokenManager tokenManager;

    public RegisterScriptHandler(RegistrationAuthTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        String id = request.getId();

        if (StringUtils.isBlank(id)) {
            return false;
        }

        boolean yaml = id.endsWith(".yaml");
        if (yaml) {
            id = id.substring(0, id.length()-5);
        }

        RegistrationAuthTokenManager.TokenAccount account = tokenManager.validateToken(id);
        if (account == null) {
            return false;
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("CATTLE_URL", getUrl(request));
        tokens.put("CATTLE_REGISTRATION_URL", ClusterOutputFilter.regUrl(id));
        tokens.put("CATTLE_REGISTRATION_ACCESS_KEY", CredentialConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN);
        tokens.put("CATTLE_REGISTRATION_SECRET_KEY", id);
        tokens.put("CATTLE_AGENT_IMAGE", IMAGE.get());
        tokens.put("CATTLE_AGENT_IP", request.getClientIp());
        tokens.put("RANDOM", UUID.randomUUID().toString().substring(0,8));

        for (String key : new String[] {"CATTLE_URL", "CATTLE_REGISTRATION_ACCESS_KEY", "CATTLE_REGISTRATION_SECRET_KEY"}) {
            String value = tokens.get(key);
            tokens.put(key + "_ENCODED", Base64.getEncoder().encodeToString(value.getBytes(ObjectUtils.UTF8)));
        }

        String script = getScript(yaml);

        if (script == null) {
            return false;
        }

        script = new StrSubstitutor(tokens).replace(script);
        request.getServletContext().getResponse().getOutputStream().write(script.getBytes("UTF-8"));

        return true;
    }

    protected String getUrl(ApiRequest request) {
        String url = URL.get();

        if (!StringUtils.isBlank(url)) {
            return url;
        }

        if (ServerContext.isCustomApiHost()) {
            return ServerContext.getHostApiBaseUrl(BaseProtocol.HTTP);
        }

        return request.getUrlBuilder().version(request.getVersion()).toExternalForm();
    }

    protected String getScript(boolean yaml) {
        InputStream is = null;
        String file = yaml ? YAML.get() : SCRIPT.get();

        try {
            is = getClass().getClassLoader().getResourceAsStream(file);
            if (is == null) {
                return null;
            }

            return IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            log.error("Failed to read [{}]", file, e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public RegistrationAuthTokenManager getTokenManager() {
        return tokenManager;
    }

}
