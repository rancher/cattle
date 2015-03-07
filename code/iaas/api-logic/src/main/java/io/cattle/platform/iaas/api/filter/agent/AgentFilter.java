package io.cattle.platform.iaas.api.filter.agent;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.cattle.platform.object.util.DataUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicStringProperty;

public class AgentFilter extends AbstractDefaultResourceManagerFilter {

    private static final Logger log = LoggerFactory.getLogger(AgentFilter.class);

    private static final DynamicBooleanProperty ASSIGN_AGENT_URI = ArchaiusUtil.getBoolean("agent.filter.assign.uri");
    private static final DynamicBooleanProperty TRY_DNS = ArchaiusUtil.getBoolean("agent.filter.default.uri.reverse.dns");
    private static final DynamicStringProperty URI_FORMAT = ArchaiusUtil.getString("agent.filter.default.uri");
    private static final DynamicStringProperty USER = ArchaiusUtil.getString("agent.filter.default.user");

    ResourceManagerLocator locator;
    AgentDao agentDao;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Agent.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String ip = request.getClientIp();
        Agent agent = request.proxyRequestObject(Agent.class);
        /*
         * This ensures that the accountId is always set from the request and
         * never overwritten by the default accountId setting logic. In the
         * situation in which the client doesn't have access to the accountId
         * field, the result will be null, which is correct. You want it to be
         * null so that the AgentCreate logic will create an account for this
         * Agent
         */
        agent.setAccountId(agent.getAccountId());
        String uri = agent.getUri();
        String user = DataUtils.getFieldFromRequest(request, AgentConstants.USER, String.class);

        if (uri == null) {
            uri = getUri(user, ip);
            if (uri != null) {
                isUnique(uri);
                agent.setUri(uri);
            }
        }

        return super.create(type, request, next);
    }

    protected void isUnique(String uri) {
        Agent existing = agentDao.findNonRemovedByUri(uri);
        if (existing != null) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, "uri");
        }
    }

    protected String getUri(String user, String ip) {
        if (ip == null) {
            return null;
        }

        if (user == null) {
            user = USER.get();
        }

        if (!ASSIGN_AGENT_URI.get()) {
            return null;
        }

        if (TRY_DNS.get()) {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                ip = addr.getCanonicalHostName();
            } catch (UnknownHostException e) {
                log.warn("Failed to do a reverse DNS lookup on [{}], using raw IP for uri", ip);
            }
        }

        return String.format(URI_FORMAT.get(), user, ip);
    }

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

    public AgentDao getAgentDao() {
        return agentDao;
    }

    @Inject
    public void setAgentDao(AgentDao agentDao) {
        this.agentDao = agentDao;
    }

}