package io.cattle.platform.configitem.api.manager;

import io.cattle.platform.api.auth.Policy;
import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.configitem.api.model.ConfigContent;
import io.cattle.platform.configitem.api.request.ApiConfigItemRequest;
import io.cattle.platform.configitem.model.DefaultItemVersion;
import io.cattle.platform.configitem.model.ItemVersion;
import io.cattle.platform.configitem.server.model.Request;
import io.cattle.platform.configitem.server.service.ConfigItemServer;
import io.github.ibuildthecloud.gdapi.context.ApiContext;
import io.github.ibuildthecloud.gdapi.exception.ClientVisibleException;
import io.github.ibuildthecloud.gdapi.exception.ValidationErrorException;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Schema.Method;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;
import io.github.ibuildthecloud.gdapi.util.ResponseCodes;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.io.IOException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigContentManager extends AbstractNoOpResourceManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigContentManager.class);

    ConfigItemServer configItemServer;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ConfigContent.class };
    }

    @Override
    protected Object updateInternal(String type, String id, Object obj, ApiRequest request) {
        return handle(id, request, true);
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        ApiRequest request = ApiContext.getContext().getApiRequest();
        if ( Method.GET.isMethod(request.getMethod()) ) {
            return handle(id, request, false);
        } else {
            return new Object();
        }
    }

    protected Object handle(String id, ApiRequest request, boolean apply) {
        ConfigContent content = request.proxyRequestObject(ConfigContent.class);
        ItemVersion version = apply ? DefaultItemVersion.fromString(content.getVersion()) : null;
        Long agentId = request.proxyRequestObject(ConfigContent.class).getAgentId();

        if ( agentId == null ) {
            String agentIdStr = ApiUtils.getPolicy().getOption(Policy.AGENT_ID);
            if ( agentIdStr == null ) {
                throw new ValidationErrorException(ValidationErrorCodes.MISSING_REQUIRED, "agentId");
            } else {
                agentId = new Long(agentIdStr);
            }
        }

        try {
            ApiConfigItemRequest configRequest = new ApiConfigItemRequest(id, agentId, version, request);
            configItemServer.handleRequest(configRequest);
            if ( configRequest.getResponseCode() == Request.NOT_FOUND ) {
                throw new ClientVisibleException(ResponseCodes.NOT_FOUND);
            }
        } catch (IOException e) {
            log.error("Failed to download [{}] for agent [{}]", id, agentId, e);
            throw new IllegalStateException("Failed to download [" + id + "] for agent [" + agentId + "]", e);
        }

        return new Object();
    }

    public ConfigItemServer getConfigItemServer() {
        return configItemServer;
    }

    @Inject
    public void setConfigItemServer(ConfigItemServer configItemServer) {
        this.configItemServer = configItemServer;
    }

}
