package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.AgentTable.AGENT;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.resource.ResourceMonitor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.servicediscovery.service.lbservice.LoadBalancerServiceLookup;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class LoadBalancerServiceUpdateConfig extends AbstractObjectProcessLogic implements ProcessPostListener, Priority {
    public static final String CONFIG_ITEM_NAME = "haproxy";

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ConfigItemStatusManager statusManager;

    @Inject
    ResourceMonitor resourceMonitor;

    @Inject
    List<LoadBalancerServiceLookup> lbLookups;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    InstanceDao instanceDao;


    @Override
    public String[] getProcessNames() {
        return new String[] {
                InstanceConstants.PROCESS_START,
                InstanceConstants.PROCESS_STOP,
                InstanceConstants.PROCESS_RESTART,
                ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_CREATE,
                ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_REMOVE,
                ServiceConstants.PROCESS_SERVICE_CONSUME_MAP_UPDATE,
                ServiceConstants.PROCESS_SERVICE_UPDATE,
                ServiceConstants.PROCESS_SERVICE_ACTIVATE,
                ServiceConstants.PROCESS_SERVICE_DEACTIVATE,
                ServiceConstants.PROCESS_SERVICE_EXPOSE_MAP_CREATE,
                ServiceConstants.PROCESS_SERVICE_EXPOSE_MAP_REMOVE,
                "certificate.update"
        };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        List<? extends Service> lbServices = new ArrayList<>();

        for (LoadBalancerServiceLookup lookup : lbLookups) {
            lbServices = lookup.getLoadBalancerServices(state.getResource());
            if (lbServices != null && !lbServices.isEmpty()) {
                break;
            }
        }
        if (lbServices == null || lbServices.isEmpty()) {
            return null;
        }
        
        List<Service> activeLbServices = new ArrayList<>();

        // update config only for active services
        for (Service lbService : lbServices) {
            if (sdService.isActiveService(lbService)) {
                activeLbServices.add(lbService);
            }
        }

        List<? extends Instance> lbInstances = getLoadBalancerServiceInstances(state, process, activeLbServices);

        updateLoadBalancerConfigs(state, lbInstances, process);

        return null;
    }

    private void updateLoadBalancerConfigs(ProcessState state, List<? extends Instance> lbInstances,
            ProcessInstance process) {
        Map<Long, Agent> agents = new HashMap<>();
        for (Instance lbInstance : lbInstances) {
            Agent agent = objectManager.findAny(Agent.class, AGENT.ID, lbInstance.getAgentId(), AGENT.REMOVED, null);
            if (agent != null) {
                agents.put(agent.getId(), agent);
            }
        }

        for (Agent agent : agents.values()) {
            ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state,
                    getContext(agent, process));
            request = before(request, agent);
            ConfigUpdateRequestUtils.setRequest(request, state, getContext(agent, process));
        }
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }

    protected ConfigUpdateRequest before(ConfigUpdateRequest request, Agent agent) {
        if (request == null) {
            request = ConfigUpdateRequest.forResource(Agent.class, agent.getId());
            request.addItem(CONFIG_ITEM_NAME).withApply(true).withIncrement(true).setCheckInSyncOnly(true);
        }
        statusManager.updateConfig(request);
        return request;
    }

    public String getContext(Agent agent, ProcessInstance instance) {
        return String.format("AgentUpdateConfig:%s:%s", agent.getId(), instance.getId());
    }

    private List<? extends Instance> getLoadBalancerServiceInstances(ProcessState state,
            ProcessInstance process, List<Service> activeLbServices) {
        List<Instance> lbInstances = new ArrayList<>();
        for (Service lbService : activeLbServices) {
            lbInstances.addAll(instanceDao.findInstancesFor(lbService));
        }
        return lbInstances;
    }
}
