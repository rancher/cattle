package io.cattle.platform.simple.allocator;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.constraint.ValidHostsConstraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.service.AbstractAllocator;
import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.allocator.service.AllocationRequest;
import io.cattle.platform.allocator.service.Allocator;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;
import io.cattle.platform.simple.allocator.network.NetworkAllocationCandidates;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class SimpleAllocator extends AbstractAllocator implements Allocator, Named {

    private static final String FORCE_RESERVE = "force";
    private static final String HOST_ID = "hostID";
    private static final String RESOURCE_REQUESTS = "resourceRequests";
    private static final String SCHEDULER_REQUEST_DATA_NAME = "schedulerRequest";
    private static final String SCHEDULER_PRIORITIZE_EVENT = "scheduler.prioritize";
    private static final String SCHEDULER_RESERVE_EVENT = "scheduler.reserve";
    private static final String SCHEDULER_RELEASE_EVENT = "scheduler.release";
    private static final String SCHEDULER_PRIORITIZE_RESPONSE = "prioritizedCandidates";

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
    protected LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt) {
        if (attempt != null) {
            return new AccountAllocatorLock(attempt.getAccountId());
        }

        return new SimpleAllocatorLock();
    }

    @Override
    protected Iterator<AllocationCandidate> getCandidates(AllocationAttempt request) {
        List<Long> volumeIds = new ArrayList<Long>(request.getVolumeIds());

        QueryOptions options = new QueryOptions();

        options.setAccountId(request.getAccountId());

        for (Constraint constraint : request.getConstraints()) {
            if (constraint instanceof ValidHostsConstraint) {
                options.getHosts().addAll(((ValidHostsConstraint)constraint).getHosts());
            }
        }

        if (request.getInstance() == null) {
            return simpleAllocatorDao.iteratorPools(volumeIds, options);
        } else {
            List<String> orderedHostUUIDs = null;
            if (DataAccessor.fields(request.getInstance()).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID) == null) {
                orderedHostUUIDs = callExternalSchedulerForHosts(request.getInstance());
            }
            return simpleAllocatorDao.iteratorHosts(orderedHostUUIDs, volumeIds, options, getCallback(request));
        }
    }

    @Override
    protected void releaseAllocation(Instance instance) {
        // This is kind of strange logic to remove deallocatefor every instance host map, but in truth there will be only one ihm
        Map<String, List<InstanceHostMap>> maps = allocatorDao.getInstanceHostMapsWithHostUuid(instance.getId());
        for (Map.Entry<String, List<InstanceHostMap>>entry : maps.entrySet()) {
            for (InstanceHostMap map : entry.getValue()) {
                DataAccessor data = DataAccessor.fromDataFieldOf(map)
                                        .withScope(AllocatorDao.class)
                                        .withKey("deallocated");
                Boolean done = data.as(Boolean.class);
                if ( done == null || ! done.booleanValue() ) {
                    allocatorDao.releaseAllocation(instance, map);
                    callExternalSchedulerToRelease(instance, entry.getKey());
                }
            }
        }
    }

    @Override
    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        if (attempt.getInstance() != null) {
            Long existingHost = attempt.getHostId();
            Long newHost = candidate.getHost();
            if (existingHost == null && newHost != null) {
                callExternalSchedulerToReserve(attempt, candidate);
            }
        }
        return allocatorDao.recordCandidate(attempt, candidate);
    }

    void callExternalSchedulerToReserve(AllocationAttempt attempt, AllocationCandidate candidate) {
        Long agentId = getAgentResource(attempt.getInstance());
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildExternalSchedulerEvent(attempt.getInstance(), SCHEDULER_RESERVE_EVENT);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, candidate.getHostUuid());

                if (DataAccessor.fields(attempt.getInstance()).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID) != null) {
                    reqData.put(FORCE_RESERVE, true);
                }

                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                agent.callSync(schedulerEvent);
            }
        }
    }

    void callExternalSchedulerToRelease(Instance instance, String hostUuid) {
        Long agentId = getAgentResource(instance);
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildExternalSchedulerEvent(instance, SCHEDULER_RELEASE_EVENT);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                agent.callSync(schedulerEvent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> callExternalSchedulerForHosts(Instance instance) {
        List<String> hosts = null;
        Long agentId = getAgentResource(instance);
        if (agentId != null) {
            EventVO<Map<String, Object>> schedulerEvent = buildExternalSchedulerEvent(instance, SCHEDULER_PRIORITIZE_EVENT);

            if (schedulerEvent != null) {
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = agent.callSync(schedulerEvent);
                hosts = (List<String>)CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
            }
        }
        return hosts;
    }

    EventVO<Map<String, Object>> buildExternalSchedulerEvent(Instance instance, String eventName) {
        List<ResourceRequest> resourceRequests = gatherResourceRequests(instance);
        if (resourceRequests == null || resourceRequests.isEmpty()) {
            return null;
        }
        Map<String, Object> eventData = new HashMap<String, Object>();
        Map<String, Object> reqData = new HashMap<>();
        reqData.put(RESOURCE_REQUESTS, resourceRequests);
        eventData.put(SCHEDULER_REQUEST_DATA_NAME, reqData);
        EventVO<Map<String, Object>> schedulerEvent = EventVO.<Map<String, Object>>newEvent(eventName).withData(eventData);
        schedulerEvent.setResourceType(SCHEDULER_REQUEST_DATA_NAME);
        return schedulerEvent;
    }

    private List<ResourceRequest> gatherResourceRequests(Instance instance) {
        List<ResourceRequest> requests = new ArrayList<>();
        Long memory = DataAccessor.fieldLong(instance, "memory");
        if (memory != null) {
            ResourceRequest rr = new ResourceRequest();
            rr.setAmount(memory);
            rr.setResource("memory");
            requests.add(rr);
        }

        return requests;
    }

    private Long getAgentResource(Instance instance) {
        Long accountId = instance.getAccountId();
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        Long agentId = agentIds.size() == 0 ? null : agentIds.get(0);
        if ((instance instanceof Instance) && agentIds.contains(instance.getAgentId())) {
            return null;
        }
        return agentId;
    }

    protected AllocationCandidateCallback getCallback(AllocationAttempt request) {
        if (request.getInstance() == null) {
            return null;
        }

        return new NetworkAllocationCandidates(objectManager, request);
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
