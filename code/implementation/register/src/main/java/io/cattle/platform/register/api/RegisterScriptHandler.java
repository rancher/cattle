package io.cattle.platform.register.api;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.model.Account;
import io.cattle.platform.iaas.api.request.handler.ScriptsHandler;
import io.cattle.platform.register.auth.RegistrationAuthTokenManager;
import io.cattle.platform.register.util.RegisterConstants;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.server.context.ServerContext.BaseProtocol;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class RegisterScriptHandler implements ScriptsHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterScriptHandler.class);

    private static final DynamicStringProperty IMAGE = ArchaiusUtil.getString("agent.image");
    private static final DynamicStringProperty SCRIPT = ArchaiusUtil.getString("agent.instance.register.script");
    private static final DynamicStringProperty URL = ArchaiusUtil.getString("agent.instance.register.url");

    RegistrationAuthTokenManager tokenManager;

    @Override
    public boolean handle(ApiRequest request) throws IOException {
        String id = request.getId();

        if (StringUtils.isBlank(id)) {
            return false;
        }

        Account account = tokenManager.validateToken(id);

        if (account == null) {
            return false;
        }

        Map<String, String> tokens = new HashMap<String, String>();
        tokens.put("CATTLE_URL", getUrl(request));
        tokens.put("CATTLE_REGISTRATION_ACCESS_KEY", RegisterConstants.KIND_CREDENTIAL_REGISTRATION_TOKEN);
        tokens.put("CATTLE_REGISTRATION_SECRET_KEY", id);
        tokens.put("CATTLE_AGENT_IMAGE", IMAGE.get());
        tokens.put("CATTLE_AGENT_IP", request.getClientIp());

        String script = getScript();

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

    protected String getScript() {
        InputStream is = null;

        try {
            is = getClass().getClassLoader().getResourceAsStream(SCRIPT.get());
            if (is == null) {
                return null;
            }

            return IOUtils.toString(is, "UTF-8");
        } catch (IOException e) {
            log.error("Failed to read [{}]", SCRIPT.get(), e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public RegistrationAuthTokenManager getTokenManager() {
        return tokenManager;
    }

    @Inject
    public void setTokenManager(RegistrationAuthTokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

}
