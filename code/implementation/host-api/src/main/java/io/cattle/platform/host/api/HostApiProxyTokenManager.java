package io.cattle.platform.host.api;

import static io.cattle.platform.server.context.ServerContext.*;
import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class HostApiProxyTokenManager extends AbstractNoOpResourceManager {

    private static final String VERIFY_AGENT = "CantVerifyAgent";

    @Inject
    TokenService tokenService;

    @Inject
    AgentDao agentDao;

    @Inject
    ObjectManager objectManager;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { HostApiProxyTokenImpl.class };
    }

    @Override
    protected Object createInternal(String type, ApiRequest request) {
        HostApiProxyToken p = request.proxyRequestObject(HostApiProxyToken.class);
        validate(p);

        HostApiProxyTokenImpl token = new HostApiProxyTokenImpl();
        token.setToken(getToken(p.getReportedUuid()));
        token.setReportedUuid(p.getReportedUuid());

        StringBuilder buffer = new StringBuilder();
        switch (getHostApiProxyMode()) {
        case HOST_API_PROXY_MODE_HA:
            if (StringUtils.isNotBlank(HostApiUtils.HOST_API_PROXY_HOST.get())) {
                String scheme = StringUtils.startsWithIgnoreCase(request.getResponseUrlBase(), "https") ? "wss://" : "ws://";
                buffer.append(scheme).append(HostApiUtils.HOST_API_PROXY_HOST.get());
                break;
            }
            // Purposefully fall through
        case HOST_API_PROXY_MODE_EMBEDDED:
            if (ServerContext.isCustomApiHost()) {
                buffer.append(ServerContext.getHostApiBaseUrl(ServerContext.BaseProtocol.WEBSOCKET));
            } else {
                buffer.append(request.getResponseUrlBase().replaceFirst("http", "ws"));
            }
            break;


        case HOST_API_PROXY_MODE_OFF:
            throw new ClientVisibleException(501, "HostApiProxyDisabled");
        }

        if (buffer.length() <= 0) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "CantConstructUrl");
        }

        if ('/' == buffer.charAt(buffer.length() - 1)) {
            buffer.deleteCharAt(buffer.length() - 1);
        }

        String url = buffer.append(HostApiUtils.HOST_API_PROXY_BACKEND.get()).toString();
        token.setUrl(url);
        return token;
    }

    protected String getToken(String reportedUuid) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(HostConstants.FIELD_REPORTED_UUID, reportedUuid);
        return tokenService.generateToken(data);
    }

    protected void validate(HostApiProxyToken proxyToken) {
        String reportedUuid = proxyToken.getReportedUuid();

        Policy policy = ApiUtils.getPolicy();
        Agent agent = objectManager.loadResource(Agent.class, policy.getOption(Policy.AGENT_ID));
        if (agent == null) {
            throw new ClientVisibleException(ResponseCodes.FORBIDDEN, VERIFY_AGENT);
        }
        Map<String, Host> hosts = agentDao.getHosts(agent.getId());
        Host host = hosts.get(reportedUuid);
        if (host == null) {
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, HostConstants.FIELD_REPORTED_UUID);
        }
    }
}
