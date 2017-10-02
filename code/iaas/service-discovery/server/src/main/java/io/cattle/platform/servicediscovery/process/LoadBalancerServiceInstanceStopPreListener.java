package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.core.addon.LbConfig;
import io.cattle.platform.core.addon.PortRule;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.ServiceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.dao.InstanceDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Service;
import io.cattle.platform.core.model.ServiceExposeMap;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.servicediscovery.api.util.ServiceDiscoveryUtil;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;

public class LoadBalancerServiceInstanceStopPreListener extends AgentBasedProcessHandler implements ProcessPreListener, Priority {

    @Inject
    JsonMapper jsonMapper;

    @Inject
    ServiceDiscoveryService sdService;

    @Inject
    InstanceDao instanceDao;

    @Inject
    GenericMapDao mapDao;

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerServiceInstanceStopPreListener.class);
    private static final String LABEL_SERVICE_LAUNCH_CONFIG = "io.rancher.service.launch.config";

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (instance == null) {
            return null;
        }
        // container.stop -> find container’s service -> find all the LB services of the account of the container
        // service -> filter out those services having lbConfig.portRules having serviceId = container’s service
        Service service = loadService(instance);
        if (service == null) {
            return null;
        }

        // read the launchConfigLabel on the container
        Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
        String launchConfigName = labels.get(LABEL_SERVICE_LAUNCH_CONFIG) != null ? labels.get(LABEL_SERVICE_LAUNCH_CONFIG).toString() : null;
        Integer drainTimeout = (Integer) ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                ServiceConstants.FIELD_DRAIN_TIMEOUT);
        if (drainTimeout == null || (drainTimeout == 0)) {
            return null;
        }

        List<Service> activeLbServices = lookupAllActiveLBServices(service);
        List<? extends Instance> lbInstances = getLBServiceInstances(activeLbServices);
        // send drain event to each lbInstance and wait for reply upto timeout.
        if (!lbInstances.isEmpty()) {
            objectManager.setFields(instance, ObjectMetaDataManager.TRANSITIONING_MESSAGE_FIELD, "Draining...");
        }

        String targetIpAddress = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);

        Map<Instance, ListenableFuture<? extends Event>> drainFutures = new HashMap<>();

        for (Instance lbInstance : lbInstances) {
            ListenableFuture<? extends Event> future = drainBackend(lbInstance, instance, targetIpAddress, drainTimeout.toString());
            if (future != null) {
                drainFutures.put(lbInstance, future);
            }
        }

        for (Map.Entry<Instance, ListenableFuture<? extends Event>> entry : drainFutures.entrySet()) {
            Instance lbInstance = entry.getKey();
            RemoteAgent agent = agentLocator.lookupAgent(lbInstance);
            ListenableFuture<? extends Event> future = entry.getValue();
            try {
                AsyncUtils.get(future);
                log.debug("Got reply to event from AgentId for the target: " + agent.getAgentId());
            } catch (EventExecutionException e) {
                log.debug("Got error: {}, to event from LB Agent: {} ", e, agent.getAgentId());
                throw new ExecutionException(e);
            }
        }
        return null;
    }

    protected ListenableFuture<? extends Event> drainBackend(Instance lbInstance, Instance targetInstance, String targetIpAddress, String drainTimeout) {
        RemoteAgent agent = agentLocator.lookupAgent(lbInstance);
        if (agent == null) {
            return null;
        }
        Map<String, Object> drainInfo = new HashMap<>();
        drainInfo.put("targetIPaddress", targetIpAddress);
        if (drainTimeout != null)
            drainInfo.put("drainTimeout", drainTimeout);

        Event event = new EventVO<>("target.drain").withData(drainInfo).withResourceType(getObjectManager().getType(targetInstance))
                .withResourceId(ObjectUtils.getId(targetInstance).toString());

        log.debug("Sending event to AgentId for the target: " + agent.getAgentId());
        return agent.call(event);
    }

    private Service loadService(Instance instance) {
        ServiceExposeMap smap = objectManager.findAny(ServiceExposeMap.class,
                SERVICE_EXPOSE_MAP.INSTANCE_ID, instance.getId(),
                SERVICE_EXPOSE_MAP.REMOVED, null);
        if (smap == null) {
            return null;
        }
        Service service = getObjectManager().loadResource(Service.class, smap.getServiceId());
        if (service == null) {
            return null;
        }
        return service;
    }

    @Override
    protected void preProcessEvent(EventVO<?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource,
            Object agentResource) {
        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        if (!data.containsKey("targetIPaddress")) {
            Instance instance = (Instance) state.getResource();
            String ipAddress = DataAccessor.fieldString(instance, InstanceConstants.FIELD_PRIMARY_IP_ADDRESS);
            data.put("targetIPaddress", ipAddress);
        }

        if (!data.containsKey("drainTimeout")) {
            Instance instance = (Instance) state.getResource();
            Service service = loadService(instance);
            if (service != null) {
                //read the launchConfigLabel on the container
                Map<String, Object> labels = DataAccessor.fieldMap(instance, InstanceConstants.FIELD_LABELS);
                String launchConfigName = labels.get(LABEL_SERVICE_LAUNCH_CONFIG) != null ? labels.get(LABEL_SERVICE_LAUNCH_CONFIG).toString() : null;
                Object drainTimeout = ServiceDiscoveryUtil.getLaunchConfigObject(service, launchConfigName,
                        ServiceConstants.FIELD_DRAIN_TIMEOUT);
                if (drainTimeout != null)
                    data.put("drainTimeout", drainTimeout.toString());
            }
        }
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);
    }

    private List<Service> lookupAllActiveLBServices(Service targetService) {
        List<Service> lbServices = new ArrayList<>();

        List<Service> lbServicesForAccount = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, targetService.getAccountId(),
                SERVICE.REMOVED, null, SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE);

        // find all lbServices that have the given service as a target by looking at PortRules

        for (Service lbService : lbServicesForAccount) {
            if (sdService.isActiveService(lbService)) {
                LbConfig lbConfig = DataAccessor.field(lbService, ServiceConstants.FIELD_LB_CONFIG,
                        jsonMapper, LbConfig.class);
                if (lbConfig != null && lbConfig.getPortRules() != null) {
                    for (PortRule rule : lbConfig.getPortRules()) {
                        if (!StringUtils.isEmpty(rule.getServiceId()) && Long.valueOf(rule.getServiceId()).equals(targetService.getId())) {
                            lbServices.add(lbService);
                        } else {
                            if (sdService.isSelectorLinkMatch(rule.getSelector(), targetService)) {
                                lbServices.add(lbService);
                            }
                            if (sdService.isSelectorLinkMatch(targetService.getSelectorLink(), lbService)) {
                                lbServices.add(lbService);
                            }
                        }
                    }
                }
            }
        }
        return lbServices;
    }

    private List<? extends Instance> getLBServiceInstances(List<Service> activeLbServices) {
        List<Instance> lbInstances = new ArrayList<>();
        for (Service lbService : activeLbServices) {
            lbInstances.addAll(instanceDao.findInstancesFor(lbService));
        }
        return lbInstances;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { InstanceConstants.PROCESS_STOP };
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}