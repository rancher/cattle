package io.cattle.platform.servicediscovery.process;

import static io.cattle.platform.core.model.tables.ServiceExposeMapTable.*;
import static io.cattle.platform.core.model.tables.ServiceTable.*;
import io.cattle.platform.agent.RemoteAgent;
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
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.servicediscovery.service.ServiceDiscoveryService;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class LoadBalancerServiceInstanceStopPreListener extends AgentBasedProcessHandler implements ProcessPreListener, Priority {
    
    @Inject
    JsonMapper jsonMapper;
    
    @Inject
    ServiceDiscoveryService sdService;
    
    @Inject
    InstanceDao instanceDao;
    
    @Inject
    GenericMapDao mapDao;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance) state.getResource();
        if (instance == null) {
            return null;
        }
        //container.stop -> find container’s service -> find all the LB services of the account of the container service -> filter out those services having lbConfig.portRules having serviceId = container’s service
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
        List<Service> activeLbServices = lookupAllActiveLBServices(service);
        List<? extends Instance> lbInstances = getLBServiceInstances(activeLbServices);
        //send drain event to each lbInstance and wait for reply upto timeout.
        for (Instance lbInstance : lbInstances) {
            RemoteAgent agent = agentLocator.lookupAgent(lbInstance);
            if (agent == null)
                continue;

            handleEvent(state, process, instance, instance, lbInstance, agent);

            /*
             * Instead of the sync call above, do we do this? How to wait for all futures to finish, the below will just
             * finish the for loop and exit:
             * 
             * ObjectSerializer serializer = getObjectSerializer(instance);
             * Map<String, Object> data = serializer == null ? null : serializer.serialize(instance);
             * EventVO<Object> event = EventVO.newEvent(getCommandName() == null ? process.getName() : getCommandName())
             * .withData(data)
             * .withResourceType(getObjectManager().getType(instance))
             * .withResourceId(ObjectUtils.getId(instance).toString());
             * 
             * final ListenableFuture<? extends Event> future = agent.call(event, new EventCallOptions(new Integer(0),
             * new Long(5000)));
             * 
             * future.addListener(new NoExceptionRunnable() {
             * 
             * @Override
             * protected void doRun() {
             * try {
             * Event resp = future.get();
             * String id = resp.getId();
             * } catch (AgentRemovedException e) {
             * // ignore this?
             * } catch (TimeoutException e) {
             * throw new ProcessDelayException(new Date(System.currentTimeMillis() + 15000));
             * } catch (Exception e) {
             * // do nothing or throw processDelay?
             * }
             * }
             * }, MoreExecutors.sameThreadExecutor());
             */
        }
        return null;
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
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);
    }

    private List<Service> lookupAllActiveLBServices(Service targetService) {
        List<Service> lbServices = new ArrayList<>();

        List<Service> lbServicesForAccount = objectManager.find(Service.class, SERVICE.ACCOUNT_ID, targetService.getAccountId(),
                SERVICE.REMOVED, null, SERVICE.KIND, ServiceConstants.KIND_LOAD_BALANCER_SERVICE);
        
        //find all lbServices that have the given service as a target by looking at PortRules
        
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
