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

import java.net.MalformedURLException;
import java.net.URL;
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

        String apiHost = ServerContext.HOST.get();
        if (StringUtils.isBlank(apiHost)) {
            try {
                String responseBaseUrl = request.getResponseUrlBase();
                URL url = new URL(responseBaseUrl);
                if (StringUtils.isNotBlank(url.getHost())) {
                    apiHost = url.getHost();
                    if (url.getPort() != -1) {
                        apiHost = apiHost + ":" + url.getPort();
                    }
                }
            } catch (MalformedURLException e) {
                throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "CantConstructUrl");
            }
        }

        if (StringUtils.isBlank(apiHost)) {
            throw new ClientVisibleException(ResponseCodes.INTERNAL_SERVER_ERROR, "CantConstructUrl");
        }

        String apiProxyScheme = ServerContext.HOST_API_PROXY_SCHEME.get();
        token.setUrl(apiProxyScheme + "://" + apiHost + "/v1/connectbackend");
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
