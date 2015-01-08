package io.cattle.platform.agent.instance.link.process;

import io.cattle.platform.agent.instance.service.AgentInstanceManager;
import io.cattle.platform.agent.instance.service.NetworkServiceInfo;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.InstanceLinkConstants;
import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceLink;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.object.resource.ResourcePredicate;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessHandler;
import io.cattle.platform.resource.pool.PooledResource;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;
import io.cattle.platform.util.exception.ExecutionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicLongProperty;

public class AgentInstanceLinkActivate extends AbstractObjectProcessHandler {

    private static final DynamicLongProperty WAIT_TIME = ArchaiusUtil.getLong("instance.link.target.wait.time.millis");
    private static final Logger log = LoggerFactory.getLogger(AgentInstanceLinkActivate.class);

    AgentInstanceManager agentInstanceManager;
    JsonMapper jsonMapper;
    ResourcePoolManager resourcePoolManager;
    ResourceMonitor resourceMonitor;

    @Override
    public String[] getProcessNames() {
        return new String[] { "instancelink.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        InstanceLink link = (InstanceLink) state.getResource();
        Instance instance = loadResource(Instance.class, link.getInstanceId());
        Instance targetInstance = loadResource(Instance.class, link.getTargetInstanceId());

        if (instance == null) {
            return null;
        }

        NetworkServiceInfo info = agentInstanceManager.getNetworkService(instance, NetworkServiceConstants.KIND_LINK, true);

        if (info == null) {
            return null;
        }

        if (info.getIpAddress().getAddress() == null) {
            log.error("IP address for agent instance is not set");
            return null;
        }

        long timeout = DataAccessor.fromDataFieldOf(instance).withKey(InstanceLinkConstants.DATA_LINK_WAIT_TIME).withDefault(WAIT_TIME.get()).as(Long.class);

        try {
            targetInstance = resourceMonitor.waitFor(targetInstance, timeout, new ResourcePredicate<Instance>() {
                @Override
                public boolean evaluate(Instance obj) {
                    return obj.getFirstRunning() != null;
                }
            });
        } catch (TimeoutException e) {
            String message = String.format("Timeout waiting for instance link %s", link.getLinkName());
            throw new ExecutionException(message, message, link);
        }

        List<Port> ports = children(targetInstance, Port.class);

        if (ports.size() == 0) {
            return null;
        }

        List<PortSpec> result = new ArrayList<PortSpec>();
        Map<Integer, PortSpec> assigned = new HashMap<Integer, PortSpec>();
        Map<Integer, Port> portsByPrivatePort = new HashMap<Integer, Port>();

        for (Port port : ports) {
            if (port.getPrivatePort() != null) {
                portsByPrivatePort.put(port.getPrivatePort(), port);
            }
        }

        for (PortSpec spec : DataAccessor.fields(link).withKey(InstanceLinkConstants.FIELD_PORTS).withDefault(new ArrayList<PortSpec>())
                .asList(jsonMapper, PortSpec.class)) {
            if (spec.getPublicPort() != null) {
                assigned.put(spec.getPublicPort(), spec);
                if (portsByPrivatePort.remove(spec.getPrivatePort()) != null) {
                    result.add(spec);
                }
            }
        }

        List<PooledResource> portItems = resourcePoolManager.allocateResource(info.getNetworkService(), link,
                new PooledResourceOptions().withCount(ports.size()).withQualifier(ResourcePoolConstants.LINK_PORT));

        if (portItems == null) {
            throw new ExecutionException("Port allocation error", "No port available", link);
        }

        String ipAddress = info.getIpAddress().getAddress();
        for (int i = 0; i < ports.size(); i++) {
            int publicPort = Integer.parseInt(portItems.get(i).getName());
            PortSpec spec = assigned.remove(publicPort);

            if (spec == null && portsByPrivatePort.size() > 0) {
                Port port = portsByPrivatePort.values().iterator().next();
                portsByPrivatePort.remove(port.getPrivatePort());
                spec = new PortSpec(ipAddress, publicPort, port);
                result.add(spec);
            }
        }

        return new HandlerResult(InstanceLinkConstants.FIELD_PORTS, result).withShouldContinue(true);
    }

    public AgentInstanceManager getAgentInstanceManager() {
        return agentInstanceManager;
    }

    @Inject
    public void setAgentInstanceManager(AgentInstanceManager agentInstanceManager) {
        this.agentInstanceManager = agentInstanceManager;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ResourcePoolManager getResourcePoolManager() {
        return resourcePoolManager;
    }

    @Inject
    public void setResourcePoolManager(ResourcePoolManager resourcePoolManager) {
        this.resourcePoolManager = resourcePoolManager;
    }

    public ResourceMonitor getResourceMonitor() {
        return resourceMonitor;
    }

    @Inject
    public void setResourceMonitor(ResourceMonitor resourceMonitor) {
        this.resourceMonitor = resourceMonitor;
    }

}
