package io.cattle.platform.host.api;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.server.context.ServerContext;
import io.cattle.platform.token.TokenService;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.util.HashMap;
import java.util.Map;

public class HostApiProxyTokenManager extends AbstractNoOpResourceManager {

    private static final String VERIFY_AGENT = "CantVerifyAgent";

    TokenService tokenService;
    AgentDao agentDao;
    ObjectManager objectManager;

    public HostApiProxyTokenManager(TokenService tokenService, AgentDao agentDao, ObjectManager objectManager) {
        super();
        this.tokenService = tokenService;
        this.agentDao = agentDao;
        this.objectManager = objectManager;
    }

    @Override
    public Object create(String type, ApiRequest request) {
        HostApiProxyToken p = request.proxyRequestObject(HostApiProxyToken.class);
        validate(p);

        HostApiProxyTokenImpl token = new HostApiProxyTokenImpl();
        token.setToken(getToken(p.getReportedUuid()));
        token.setReportedUuid(p.getReportedUuid());

        StringBuilder buffer = new StringBuilder();
        if (ServerContext.isCustomApiHost()) {
            buffer.append(ServerContext.getHostApiBaseUrl(ServerContext.BaseProtocol.WEBSOCKET));
        } else {
            buffer.append(request.getResponseUrlBase().replaceFirst("http", "ws"));
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
        Map<String, Object> data = new HashMap<>();
        data.put(AgentConstants.REPORTED_UUID, reportedUuid);
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
            throw new ValidationErrorException(ValidationErrorCodes.INVALID_REFERENCE, AgentConstants.REPORTED_UUID);
        }
    }
}
