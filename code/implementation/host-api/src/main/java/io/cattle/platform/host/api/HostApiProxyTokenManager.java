package io.cattle.platform.host.api;

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

        HostApiProxyTokenImpl i = new HostApiProxyTokenImpl();
        i.setToken(getToken(p.getReportedUuid()));
        i.setReportedUuid(p.getReportedUuid());
        // TODO Handle unset host.api setting
        String apiHost = ServerContext.HOST.get();
        String apiProxyScheme = ServerContext.HOST_API_PROXY_SCHEME.get();
        i.setUrl(apiProxyScheme + "://" + apiHost + "/v1/connectbackend");
        return i;
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