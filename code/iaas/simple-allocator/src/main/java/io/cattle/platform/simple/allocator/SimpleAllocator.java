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
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.constants.VolumeConstants;
import io.cattle.platform.core.dao.GenericMapDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.PortSpec;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.docker.client.DockerImage;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.simple.allocator.dao.QueryOptions;
import io.cattle.platform.simple.allocator.dao.SimpleAllocatorDao;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicStringProperty;

public class SimpleAllocator extends AbstractAllocator implements Allocator, Named {

    private static final Logger log = LoggerFactory.getLogger(SimpleAllocator.class);

    private static final DynamicStringProperty PORT_SCHEDULER_IMAGE_VERSION = ArchaiusUtil.getString("port.scheduler.image.version");

    private static final String FORCE_RESERVE = "force";
    private static final String HOST_ID = "hostID";
    private static final String RESOURCE_REQUESTS = "resourceRequests";
    private static final String CONTEXT = "context";
    private static final String SCHEDULER_REQUEST_DATA_NAME = "schedulerRequest";
    private static final String SCHEDULER_PRIORITIZE_EVENT = "scheduler.prioritize";
    private static final String SCHEDULER_RESERVE_EVENT = "scheduler.reserve";
    private static final String SCHEDULER_RELEASE_EVENT = "scheduler.release";
    private static final String SCHEDULER_PRIORITIZE_RESPONSE = "prioritizedCandidates";
    private static final String INSTANCE_RESERVATION = "instanceReservation";
    private static final String MEMORY_RESERVATION = "memoryReservation";
    private static final String CPU_RESERVATION = "cpuReservation";
    private static final String STORAGE_SIZE = "storageSize";
    private static final String PORT_RESERVATION = "portReservation";
    
    private static final String COMPUTE_POOL = "computePool";
    private static final String PORT_POOL = "portPool";
    
    private static final String BIND_ADDRESS = "bindAddress";

    private static final String PHASE = "phase";

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
                options.getHosts().addAll(((ValidHostsConstraint) constraint).getHosts());
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
                    releaseResources(instance, entry.getKey(), InstanceConstants.PROCESS_DEALLOCATE);
                }
            }
        }
    }

    @Override
    public void deallocate(Volume volume) {
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

    @Override
    public void ensureResourcesReserved(Instance instance) {
        List<Instance> instances = new ArrayList<>();
        instances.add(instance);
        List<Long> agentIds = getAgentResource(instance.getAccountId(), instances);
        String hostUuid = getHostUuid(instance);
        for (Long agentId: agentIds) {
            if (agentId != null && hostUuid != null) {
                EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, InstanceConstants.PROCESS_START,
                        instances, new HashSet<Volume>(), agentId);
                if (schedulerEvent != null) {
                    Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                    reqData.put(HOST_ID, hostUuid);
                    Long rhid = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID).as(Long.class);
                    if (rhid != null) {
                        reqData.put(FORCE_RESERVE, true);
                    }

                    RemoteAgent agent = agentLocator.lookupAgent(agentId);
                    Event eventResult = callScheduler("Error reserving resources: %s", schedulerEvent, agent);
                    if (eventResult.getData() == null) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void ensureResourcesReleased(Instance instance) {
        String hostUuid = getHostUuid(instance);
        if (hostUuid != null) {
            releaseResources(instance, hostUuid, InstanceConstants.PROCESS_STOP);
        }
    }

    private String getHostUuid(Instance instance) {
        List<? extends InstanceHostMap> maps = mapDao.findNonRemoved(InstanceHostMap.class, Instance.class, instance.getId());
        if (maps.size() > 1) {
            throw new FailedToAllocate(
                    String.format("Instance %s has %d instance host maps. Cannot reserve resources.", instance.getId(), maps.size()));
        }
        if (maps.size() == 1) {
            Host h = objectManager.loadResource(Host.class, maps.get(0).getHostId());
            return h.getUuid();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private void callExternalSchedulerToReserve(AllocationAttempt attempt, AllocationCandidate candidate) {
        List<Long> agentIds = getAgentResource(attempt.getAccountId(), attempt.getInstances());
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_RESERVE_EVENT, InstanceConstants.PROCESS_ALLOCATE, attempt.getInstances(),
                    attempt.getVolumes(), agentId);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, candidate.getHostUuid());

                if (attempt.getRequestedHostId() != null) {
                    reqData.put(FORCE_RESERVE, true);
                }

                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error reserving resources: %s", schedulerEvent, agent);
                if (eventResult.getData() == null) {
                    return;
                }
                
                List<Map<String, Object>> data = (List<Map<String, Object>>) CollectionUtils.getNestedValue(eventResult.getData(), PORT_RESERVATION);
                if (data != null) {
                    attempt.setAllocatedIPs(data);
                } 
            }
        }
    }

    private void releaseResources(Instance instance, String hostUuid, String process) {
        List<Long> agentIds = getAgentResource(instance);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildReleaseEvent(process, instance, agentId);
            if (schedulerEvent != null) {
                Map<String, Object> reqData = CollectionUtils.toMap(schedulerEvent.getData().get(SCHEDULER_REQUEST_DATA_NAME));
                reqData.put(HOST_ID, hostUuid);
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                callScheduler("Error releasing resources: %s", schedulerEvent, agent);
            }
        }
    }
    
    private void callExternalSchedulerToRelease(Volume volume) {
        String hostUuid = allocatorDao.getAllocatedHostUuid(volume);
        if (StringUtils.isEmpty(hostUuid)) {
            return;
        }
        List<Long> agentIds = getAgentResource(volume);
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildReleaseEvent(VolumeConstants.PROCESS_DEALLOCATE, volume, agentId);
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
        List<Long> agentIds = getAgentResource(attempt.getAccountId(), attempt.getInstances());
        for (Long agentId : agentIds) {
            EventVO<Map<String, Object>> schedulerEvent = buildEvent(SCHEDULER_PRIORITIZE_EVENT, InstanceConstants.PROCESS_ALLOCATE, attempt.getInstances(),
                    attempt.getVolumes(), agentId);
            if (schedulerEvent != null) {
                RemoteAgent agent = agentLocator.lookupAgent(agentId);
                Event eventResult = callScheduler("Error getting hosts for resources: %s", schedulerEvent, agent);
                if (hosts == null) {
                    hosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                } else {
                    List<String> newHosts = (List<String>) CollectionUtils.getNestedValue(eventResult.getData(), SCHEDULER_PRIORITIZE_RESPONSE);
                    hosts.retainAll(newHosts);
                }

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

    private EventVO<Map<String, Object>> buildReleaseEvent(String phase, Object resource, Long agentId) {
        List<ResourceRequest> resourceRequests = new ArrayList<>();
        if (resource instanceof Instance) {
            String schedulerVersion = getSchedulerVersion(agentId);
            addInstanceResourceRequests(resourceRequests, (Instance)resource, schedulerVersion);
        }

        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(SCHEDULER_RELEASE_EVENT, resourceRequests, resource.getClass().getSimpleName(), phase, ObjectUtils.getId(resource), null);
    }

    private EventVO<Map<String, Object>> buildEvent(String eventName, String phase, List<Instance> instances, Set<Volume> volumes, Long agentId) {
        List<ResourceRequest> resourceRequests = gatherResourceRequests(instances, volumes, agentId);
        if (resourceRequests.isEmpty()) {
            return null;
        }

        return newEvent(eventName, resourceRequests, "instance", phase, instances.get(0).getId(), instances);
    }

    private EventVO<Map<String, Object>> newEvent(String eventName, List<ResourceRequest> resourceRequests, String resourceType, String phase,
            Object resourceId, Object context) {
        Map<String, Object> eventData = new HashMap<String, Object>();
        Map<String, Object> reqData = new HashMap<>();
        reqData.put(RESOURCE_REQUESTS, resourceRequests);
        reqData.put(CONTEXT, context);
        reqData.put(PHASE, phase);
        eventData.put(SCHEDULER_REQUEST_DATA_NAME, reqData);
        EventVO<Map<String, Object>> schedulerEvent = EventVO.<Map<String, Object>> newEvent(eventName).withData(eventData);
        schedulerEvent.setResourceType(resourceType);
        schedulerEvent.setResourceId(resourceId.toString());
        return schedulerEvent;
    }

    private List<ResourceRequest> gatherResourceRequests(List<Instance> instances, Set<Volume> volumes, Long agentId) {
        List<ResourceRequest> requests = new ArrayList<>();
        String schedulerVersion = getSchedulerVersion(agentId);
        for (Instance instance : instances) {
            addInstanceResourceRequests(requests, instance, schedulerVersion);
        }

        addVolumeResourceRequests(requests, volumes.toArray(new Volume[volumes.size()]));
        return requests;
    }

    private void addVolumeResourceRequests(List<ResourceRequest> requests, Volume... volumes) {
        for (Volume v : volumes) {
            if (v.getSizeMb() != null) {
                ResourceRequest rr = new ComputeResourceRequest(STORAGE_SIZE, v.getSizeMb(), COMPUTE_POOL);
                requests.add(rr);
            }
        }
    }

    private void addInstanceResourceRequests(List<ResourceRequest> requests, Instance instance, String schedulerVersion) {
        ResourceRequest memoryRequest = populateResourceRequestFromInstance(instance, MEMORY_RESERVATION, COMPUTE_POOL, schedulerVersion);
        if (memoryRequest != null) {
            requests.add(memoryRequest);
        }

        ResourceRequest cpuRequest = populateResourceRequestFromInstance(instance, CPU_RESERVATION, COMPUTE_POOL, schedulerVersion);
        if (cpuRequest != null) {
            requests.add(cpuRequest);
        }
        
        ResourceRequest portRequests = populateResourceRequestFromInstance(instance, PORT_RESERVATION, PORT_POOL, schedulerVersion);
        if (portRequests != null) {
            requests.add(portRequests);
        }

        ResourceRequest instanceRequest = populateResourceRequestFromInstance(instance, INSTANCE_RESERVATION, COMPUTE_POOL, schedulerVersion);
        requests.add(instanceRequest);
    }

    private List<Long> getAgentResource(Long accountId, List<Instance> instances) {
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        for (Instance instance : instances) {
            if (agentIds.contains(instance.getAgentId())) {
                return new ArrayList<>();
            }
        }
        return agentIds;
    }

    private List<Long> getAgentResource(Object resource) {
        Long accountId = (Long)ObjectUtils.getAccountId(resource);
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_SCHEDULING_PROVIDER, accountId);
        // If the resource being allocated is a scheduling provider agent, return null so that we don't try to send the container to the scheduler.
        Long resourceAgentId = (Long)ObjectUtils.getPropertyIgnoreErrors(resource, "agentId");
        if (resourceAgentId != null && agentIds.contains(resourceAgentId)) {
            return new ArrayList<>();
        }
        return agentIds;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }
    
    private ResourceRequest populateResourceRequestFromInstance(Instance instance, String resourceType, String poolType, String schedulerVersion) {
        switch (resourceType) {
        case PORT_RESERVATION:
            if (useLegacyPortAllocation(schedulerVersion)) {
                return null;
            }
            PortBindingResourceRequest request = new PortBindingResourceRequest();
            request.setResource(resourceType);
            request.setInstanceId(instance.getId().toString());
            request.setResourceUuid(instance.getUuid());
            List<PortSpec> portReservation = new ArrayList<>();
            for(Port port: objectManager.children(instance, Port.class)) {
                PortSpec spec = new PortSpec();
                String bindAddress = DataAccessor.fieldString(port, BIND_ADDRESS);
                if (bindAddress != null) {
                    spec.setIpAddress(bindAddress);
                }
                spec.setPrivatePort(port.getPrivatePort());
                spec.setPublicPort(port.getPublicPort());
                String proto = StringUtils.isEmpty(port.getProtocol()) ? "tcp" : port.getProtocol();
                spec.setProtocol(proto);
                portReservation.add(spec);
            }
            if (portReservation.isEmpty()) {
                return null;
            }
            request.setPortRequests(portReservation);
            request.setType(poolType);
            return request;
        case INSTANCE_RESERVATION:
            return new ComputeResourceRequest(INSTANCE_RESERVATION, 1l, poolType);
        case MEMORY_RESERVATION:
            if (instance.getMemoryReservation() != null && instance.getMemoryReservation() > 0) {
                ResourceRequest rr = new ComputeResourceRequest(MEMORY_RESERVATION, instance.getMemoryReservation(), poolType);
                return rr;
            } 
            return null;
        case CPU_RESERVATION:
            if (instance.getMilliCpuReservation() != null && instance.getMilliCpuReservation() > 0) {
                ResourceRequest rr = new ComputeResourceRequest(CPU_RESERVATION, instance.getMilliCpuReservation(), poolType);
                return rr;
            }
            return null;
        }
        return null;
    }

    @Override
    protected boolean useLegacyPortAllocation(Long accountId, List<Instance> instances) {
        List<Long> agentIds = getAgentResource(accountId, instances);
        if (agentIds == null || agentIds.size() == 0) {
            return true;
        }
        return useLegacyPortAllocation(agentIds);
    }

    protected boolean useLegacyPortAllocation(List<Long> agentIds) {
        for(Long agentId: agentIds) {
            String schedulerVersion = getSchedulerVersion(agentId);
            if (useLegacyPortAllocation(schedulerVersion)) {
                return true;
            }
        }
        return false;
    }

    protected String getSchedulerVersion(Long agentId) {
        Instance instance = agentInstanceDao.getInstanceByAgent(agentId);
        String imageUuid = (String) DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_IMAGE_UUID).get();
        DockerImage img = DockerImage.parse(imageUuid);
        String[] imageParts = img.getFullName().split(":");
        if (imageParts.length <= 1) {
            return "";
        }
        if (!imageParts[0].equals("rancher/scheduler")) {
            return "NotApplicable";
        }
        return imageParts[imageParts.length - 1];
    }

    protected boolean useLegacyPortAllocation(String actualVersion) {
        String requiredVersion = PORT_SCHEDULER_IMAGE_VERSION.get();
        if (StringUtils.isEmpty(requiredVersion)) {
            // Property not available. Use legacy
            return true;
        }
        String[] requiredParts = requiredVersion.split("\\.");
        if (requiredParts.length < 3) {
            // Required image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }
        int requiredMajor, requiredMinor = 0;
        try {
            String majorTemp = requiredParts[0].startsWith("v") ? requiredParts[0].substring(1, requiredParts[0].length()) : requiredParts[0];
            requiredMajor = Integer.valueOf(majorTemp);
            requiredMinor = Integer.valueOf(requiredParts[1]);
        } catch (NumberFormatException e) {
            // Require image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }

        String[] actualParts = actualVersion.split("\\.");
        if (actualParts.length < 3) {
            // Image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }

        int actualMajor, actualMinor = 0;
        try {
            String majorTemp = actualParts[0].startsWith("v") ? actualParts[0].substring(1, actualParts[0].length()) : actualParts[0];
            actualMajor = Integer.valueOf(majorTemp).intValue();
            actualMinor = Integer.valueOf(actualParts[1]).intValue();
        } catch (NumberFormatException e) {
            // Image is not following semantic versioning. Assume custom, don't use legacy
            return false;
        }

        if (actualMajor < requiredMajor) {
            return true;
        } else if (actualMajor == requiredMajor && actualMinor < requiredMinor) {
            return true;
        }
        return false;
    }
}
