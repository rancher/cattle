package io.cattle.platform.simple.allocator;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.PortsConstraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.service.AbstractAllocator;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleAllocator extends AbstractAllocator implements Allocator, Named, VolumeDeallocator {

    private static final Logger log = LoggerFactory.getLogger(SimpleAllocator.class);

    private static final String FORCE_RESERVE = "force";
    private static final String HOST_ID = "hostID";
    private static final String RESOURCE_REQUESTS = "resourceRequests";
    private static final String SCHEDULER_REQUEST_DATA_NAME = "schedulerRequest";
    private static final String SCHEDULER_PRIORITIZE_EVENT = "scheduler.prioritize";
    private static final String SCHEDULER_RESERVE_EVENT = "scheduler.reserve";
    private static final String SCHEDULER_RELEASE_EVENT = "scheduler.release";
    private static final String SCHEDULER_PRIORITIZE_RESPONSE = "prioritizedCandidates";
    private static final String INSTANCE_RESERVATION = "instanceReservation";
    private static final String MEMORY_RESERVATION = "memoryReservation";
    private static final String CPU_RESERVATION = "cpuReservation";
    private static final String STORAGE_SIZE = "storageSize";
    private static final String CONTEXT = "context";

    String name = getClass().getSimpleName();

    @Inject
    SimpleAllocatorDao simpleAllocatorDao;

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Inject
    AgentLocator agentLocator;

    @Inject
    GenericMapDao mapDao;

    @Override
    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt attempt) {
        List<Long> volumeIds = new ArrayList<Long>();
        for (Volume v : attempt.getVolumes()) {
            volumeIds.add(v.getId());
        }

        QueryOptions options = new QueryOptions();

        options.setAccountId(attempt.getAccountId());

        options.setRequestedHostId(attempt.getRequestedHostId());

        for (Constraint constraint : attempt.getConstraints()) {
            if (constraint instanceof ValidHostsConstraint) {
                options.getHosts().addAll(((ValidHostsConstraint)constraint).getHosts());
            }

            if (constraint instanceof PortsConstraint) {
                options.setIncludeUsedPorts(true);
            }
        }


        List<String> orderedHostUUIDs = null;
        if (attempt.getRequestedHostId() == null) {
            orderedHostUUIDs = callExternalSchedulerForHosts(attempt);
        }
        return simpleAllocatorDao.iteratorHosts(orderedHostUUIDs, volumeIds, options);
    }

    @Override
    protected void releaseAllocation(Instance instance) {
        // This is kind of strange logic to remove deallocate for every instance host map, but in truth there will be only one ihm
        Map<String, List<InstanceHostMap>> maps = allocatorDao.getInstanceHostMapsWithHostUuid(instance.getId());
        for (Map.Entry<String, List<InstanceHostMap>> entry : maps.entrySet()) {
            for (InstanceHostMap map : entry.getValue()) {
                if (!allocatorDao.isAllocationReleased(map)) {
                    allocatorDao.releaseAllocation(instance, map);
                    callExternalSchedulerToRelease(instance, entry.getKey());
                }
            }
        }
    }

    @Override
    public void releaseAllocation(Volume volume) {
        if (!allocatorDao.isAllocationReleased(volume)) {
            allocatorDao.releaseAllocation(volume);
            callExternalSchedulerToRelease(volume);
        }
    }

    @Override
    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        Long newHost = candidate.getHost();
        if (newHost != null) {
            callExternalSchedulerToReserve(attempt, candidate);
        }
        return allocatorDao.recordCandidate(attempt, candidate);
    }

    void callExternalSchedulerToReserve(AllocationAttempt attempt, AllocationCandidate candidate) {
        Long agentId = getAgentResource(attempt.getAccountId(), attempt.getInstances());
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, attempt);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, candidate.getHostUuid());

                if (attempt.getRequestedHostId() != null) {
                    reqData.put(FORCE_RESERVE, true);
                }

                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error reserving resources: %s", schedulerEvent, agent);
            }
        }
    }

    void callExternalSchedulerToRelease(Instance instance, String hostUuid) {
        Long agentId = getAgentResource(instance);
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildReleaseEvent(instance);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error releasing resources: %s", schedulerEvent, agent);
            }
        }
    }

    void callExternalSchedulerToRelease(Volume volume) {
        String hostUuid = allocatorDao.getAllocatedHostUuid(volume);
        if (StringUtils.isEmpty(hostUuid)) {
            return;
        }
        Long agentId = getAgentResource(volume);
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildReleaseEvent(volume);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error releasing resources: %s", schedulerEvent, agent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> callExternalSchedulerForHosts(AllocationAttempt attempt) {
        List<String> hosts = null;
        Long agentId = getAgentResource(attempt.getAccountId(), attempt.getInstances());
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, attempt);

            if (schedulerEvent != null) {
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error getting hosts for resources: %s", schedulerEvent, agent);

                hosts = (List<String>)CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);

                if (hosts.isEmpty()) {
                    throw new FailedToAllocate(String.format("No healthy hosts meet the resource constraints: %s", extractResourceRequests(schedulerEvent)));
                }
            }
        }
        return hosts;
    }

    Event callScheduler(String message, EventVO<Map<String, Object>> schedulerEvent, RemoteAgent agent) {
        try {
            return agent.callSync(schedulerEvent);
        } catch (EventExecutionException e) {
            log.error("External scheduler replied with an error: {}", e.getMessage());
            throw new FailedToAllocate(String.format(message, extractResourceRequests(schedulerEvent)), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<ResourceRequest> extractResourceRequests(EventVO<Map<String, Object>> schedulerEvent) {
        return  (List<ResourceRequest>)((Map<String, Object>)schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME)).get(RESOURCE_REQUESTS);
    }

    EventVO<Map<String, Object>> buildReleaseEvent(Object resource) {
        List<ResourceRequest> resourceRequests = new ArrayList<>();
        if (resource instanceof Instance) {
            addInstanceResourceRequests(resourceRequests, (Instance)resource);
        } else if (resource instanceof Volume) {
            addVolumeResourceRequests(resourceRequests, (Volume)resource);
        }

        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(SCHEDULER_RELEASE_EVENT, resourceRequests, resource.getClass().getSimpleName(), ObjectUtils.getId(resource), null);
    }

    EventVO<Map<String, Object>> buildEvent(String eventName, AllocationAttempt attempt) {
        List<ResourceRequest> resourceRequests = gatherResourceRequests(attempt);
        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(eventName, resourceRequests, "instance", attempt.getInstances().get(0).getId(), attempt.getInstances());
    }

    EventVO<Map<String, Object>> newEvent(String eventName, List<ResourceRequest> resourceRequests, String resourceType, Object resourceId, Object context) {
        Map<String, Object> eventData = new HashMap<String, Object>();
        Map<String, Object> reqData = new HashMap<>();
        reqData.put(RESOURCE_REQUESTS, resourceRequests);
        reqData.put(CONTEXT, context);
        eventData.put(SCHEDULER_REQUEST_DATA_NAME, reqData);
        EventVO<Map<String, Object>> schedulerEvent = EventVO.<Map<String, Object>> newEvent(eventName).withData(eventData);
        schedulerEvent.setResourceType(resourceType);
        schedulerEvent.setResourceId(resourceId.toString());
        return schedulerEvent;
    }

    private List<ResourceRequest> gatherResourceRequests(AllocationAttempt attempt) {
        List<ResourceRequest> requests = new ArrayList<>();
        for (Instance instance : attempt.getInstances()) {
            addInstanceResourceRequests(requests, instance);
        }

        addVolumeResourceRequests(requests, attempt.getVolumes().toArray(new Volume[attempt.getVolumes().size()]));
        return requests;
    }

    private void addVolumeResourceRequests(List<ResourceRequest> requests, Volume... volumes) {
        for (Volume v : volumes) {
            if (v.getSizeMb() != null) {
                ResourceRequest rr = new ResourceRequest();
                rr.setAmount(v.getSizeMb());
                rr.setResource(STORAGE_SIZE);
                requests.add(rr);
            }
        }
    }

    private void addInstanceResourceRequests(List<ResourceRequest> requests, Instance instance) {
        if (instance.getMemoryReservation() != null && instance.getMemoryReservation() > 0) {
            ResourceRequest rr = new ResourceRequest();
            rr.setAmount(instance.getMemoryReservation());
            rr.setResource(MEMORY_RESERVATION);
            requests.add(rr);
        }

        if (instance.getMilliCpuReservation() != null && instance.getMilliCpuReservation() > 0) {
            ResourceRequest rr = new ResourceRequest();
            rr.setAmount(instance.getMilliCpuReservation());
            rr.setResource(CPU_RESERVATION);
            requests.add(rr);
        }

        ResourceRequest r = new ResourceRequest();
        r.setAmount(1l);
        r.setResource(INSTANCE_RESERVATION);
        requests.add(r);
    }

    private Long getAgentResource(Long accountId, List<Instance> instances) {
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        Long agentId = agentIds.size() == 0 ? null : agentIds.get(0);
        for (Instance instance : instances) {
            if (agentIds.contains(instance.getAgentId())) {
                return null;
            }
        }
        return agentId;
    }

    private Long getAgentResource(Object resource) {
        Long accountId = (Long)ObjectUtils.getAccountId(resource);
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);

        // If the resource being allocated is a scheduling provider agent, return null so that we don't try to send the container to the scheduler.
        Long resourceAgentId = (Long)ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (resourceAgentId != null && agentIds.contains(resourceAgentId)) {
            return null;
        }
        return agentIds.size() == 0 ? null : agentIds.get(0);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }
}
