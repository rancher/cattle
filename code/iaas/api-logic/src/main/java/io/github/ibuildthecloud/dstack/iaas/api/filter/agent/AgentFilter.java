package io.github.ibuildthecloud.dstack.iaas.api.filter.agent;

import static io.github.ibuildthecloud.dstack.core.model.tables.AgentTable.*;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Agent;
import io.github.ibuildthecloud.dstack.iaas.api.filter.common.AbstractDefaultResourceManagerFilter;
import io.github.ibuildthecloud.dstack.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManagerLocator;
import io.github.ibuildthecloud.gdapi.validation.ValidationErrorCodes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

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

    ResourceManagerLocator locator;

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { Agent.class };
    }

    @Override
    public Object create(String type, ApiRequest request, ResourceManager next) {
        String ip = request.getClientIp();
        Agent agent = request.proxyRequestObject(Agent.class);
        String uri = agent.getUri();

        if ( uri == null ) {
            uri = getUri(ip);
            if ( uri != null ) {
                isUnique(uri);
                agent.setUri(uri);
            }
        }

        return super.create(type, request, next);
    }

    protected void isUnique(String uri) {
        String type = locator.getType(Agent.class);
        ResourceManager rm = locator.getResourceManagerByType(type);
        List<?> result = rm.list(type, CollectionUtils.asMap((Object)AGENT.URI, uri), null);
        if ( result.size() > 0 ) {
            ValidationErrorCodes.throwValidationError(ValidationErrorCodes.NOT_UNIQUE, "uri");
        }
    }

    protected String getUri(String ip) {
        if ( ip == null ) {
            return null;
        }

        if ( ! ASSIGN_AGENT_URI.get() ) {
            return null;
        }

        if ( TRY_DNS.get() ) {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                ip = addr.getCanonicalHostName();
            } catch (UnknownHostException e) {
                log.warn("Failed to do a reverse DNS lookup on [{}], using raw IP for uri", ip);
            }
        }

        return String.format(URI_FORMAT.get(), ip);
    }

    public ResourceManagerLocator getLocator() {
        return locator;
    }

    @Inject
    public void setLocator(ResourceManagerLocator locator) {
        this.locator = locator;
    }

}