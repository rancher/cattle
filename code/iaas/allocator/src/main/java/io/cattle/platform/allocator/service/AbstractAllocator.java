package io.cattle.platform.allocator.service;

import io.cattle.platform.allocator.constraint.AllocationConstraintsProvider;
import io.cattle.platform.allocator.constraint.Constraint;
import io.cattle.platform.allocator.dao.AllocatorDao;
import io.cattle.platform.allocator.exception.FailedToAllocate;
import io.cattle.platform.allocator.exception.UnsupportedAllocation;
import io.cattle.platform.allocator.lock.AllocateResourceLock;
import io.cattle.platform.allocator.lock.AllocateVolumesResourceLock;
import io.cattle.platform.allocator.service.AllocationRequest.Type;
import io.cattle.platform.allocator.util.AllocatorUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.core.constants.AccountConstants;
import io.cattle.platform.core.constants.HostConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.core.model.Volume;
import io.cattle.platform.core.util.InstanceHelpers;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.lock.LockCallback;
import io.cattle.platform.lock.LockCallbackNoReturn;
import io.cattle.platform.lock.LockManager;
import io.cattle.platform.lock.definition.LockDefinition;
import io.cattle.platform.metrics.util.MetricsUtil;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.DataAccessor;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public abstract class AbstractAllocator implements Allocator {

    private static final Logger log = LoggerFactory.getLogger(AbstractAllocator.class);
    private static final String SCHEDULER_URL = ArchaiusUtil.getString("system.stack.scheduler.url").get();

    Timer allocateLockTimer = MetricsUtil.getRegistry().timer("allocator.allocate.with.lock");
    Timer allocateTimer = MetricsUtil.getRegistry().timer("allocator.allocate");
    Timer deallocateTimer = MetricsUtil.getRegistry().timer("allocator.deallocate");

    AllocatorDao allocatorDao;
    LockManager lockManager;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    List<AllocationConstraintsProvider> allocationConstraintProviders;

    @Override
    public boolean allocate(final AllocationRequest request) {
        if (!supports(request))
            return false;

        try {
            return lockManager.lock(new AllocateResourceLock(request), new LockCallback<Boolean>() {
                @Override
                public Boolean doWithLock() {
                    switch (request.getType()) {
                    case INSTANCE:
                        return allocateInstance(request);
                    case VOLUME:
                        return allocateVolume(request);
                    }

                    return false;
                }
            });
        } catch (UnsupportedAllocation e) {
            log.info("Unsupported allocation for [{}] : {}", this, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean deallocate(final AllocationRequest request) {
        if (!supports(request))
            return false;

        try {
            return lockManager.lock(new AllocateResourceLock(request), new LockCallback<Boolean>() {
                @Override
                public Boolean doWithLock() {
                    Context c = deallocateTimer.time();
                    try {
                        return acquireLockAndDeallocate(request);
                    } finally {
                        c.stop();
                    }
                }
            });
        } catch (UnsupportedAllocation e) {
            log.info("Unsupported allocation for [{}] : {}", this, e.getMessage());
            return false;
        }
    }

    protected boolean acquireLockAndDeallocate(final AllocationRequest request) {
        return lockManager.lock(getAllocationLock(request, null), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                return runDeallocation(request);
            }
        });
    }

    protected boolean runDeallocation(final AllocationRequest request) {
        switch (request.getType()) {
        case INSTANCE:
            return deallocateInstance(request);
        case VOLUME:
            return deallocateVolume(request);
        }

        return false;
    }

    protected boolean deallocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if (stateCheck != null) {
            return stateCheck;
        }

        releaseAllocation(instance);

        return true;
    }

    protected void releaseAllocation(Instance instance) {
        allocatorDao.releaseAllocation(instance);
    }

    protected void releaseAllocation(Volume volume) {
        allocatorDao.releaseAllocation(volume);
    }

    protected boolean allocateInstance(final AllocationRequest request) {
        final Instance instance = objectManager.loadResource(Instance.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkAllocateState(request.getResourceId(), instance.getAllocationState(), "Instance");
        if (stateCheck != null) {
            return stateCheck;
        }

        final Set<Host> hosts = new HashSet<Host>(allocatorDao.getHosts(instance));
        final Set<Volume> volumes = new HashSet<Volume>(objectManager.children(instance, Volume.class));
        volumes.addAll(InstanceHelpers.extractVolumesFromMounts(instance, objectManager));
        final Map<Volume, Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();

        for (Volume v : volumes) {
            pools.put(v, new HashSet<StoragePool>(allocatorDao.getAssociatedPools(v)));
        }

        final Set<Nic> nics = new HashSet<Nic>(objectManager.children(instance, Nic.class));
        final Map<Nic, Subnet> subnets = new HashMap<Nic, Subnet>();

        for (Nic n : nics) {
            Subnet subnet = objectManager.loadResource(Subnet.class, n.getSubnetId());
            if (subnet != null) {
                subnets.put(n, subnet);
            }
        }

        return lockManager.lock(new AllocateVolumesResourceLock(volumes), new LockCallback<Boolean>() {
            @Override
            public Boolean doWithLock() {
                AllocationAttempt attempt = new AllocationAttempt(instance, hosts, volumes, pools, nics, subnets);

                return doAllocate(request, attempt, instance);
            }
        });
    }

    protected boolean deallocateVolume(AllocationRequest request) {
        final Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkDeallocateState(request.getResourceId(), volume.getAllocationState(), "Volume");
        if (stateCheck != null) {
            return stateCheck;
        }

        releaseAllocation(volume);

        return true;
    }

    protected boolean allocateVolume(AllocationRequest request) {
        Volume volume = objectManager.loadResource(Volume.class, request.getResourceId());
        Boolean stateCheck = AllocatorUtils.checkAllocateState(request.getResourceId(), volume.getAllocationState(), "Volume");
        if (stateCheck != null) {
            return stateCheck;
        }

        Set<Volume> volumes = new HashSet<Volume>();
        volumes.add(volume);

        Map<Volume, Set<StoragePool>> pools = new HashMap<Volume, Set<StoragePool>>();
        Set<StoragePool> associatedPools = new HashSet<StoragePool>(allocatorDao.getAssociatedPools(volume));
        pools.put(volume, associatedPools);

        AllocationAttempt attempt = new AllocationAttempt(null, new HashSet<Host>(), volumes, pools, null, null);

        return doAllocate(request, attempt, volume);
    }

    protected boolean doAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        AllocationLog log = getLog(request);
        populateConstraints(attempt, log);

        Context c = allocateLockTimer.time();
        try {
            return acquireLockAndAllocate(request, attempt, deallocate);
        } finally {
            c.stop();
        }
    }

    protected boolean acquireLockAndAllocate(final AllocationRequest request, final AllocationAttempt attempt, Object deallocate) {
        final List<Constraint> finalFailedConstraints = new ArrayList<>();
        final List<AllocationCandidate> candidatesForCPUMemoryIops = new ArrayList<AllocationCandidate>();
        final List<AllocationCandidate> candidates = new ArrayList<AllocationCandidate>();
        lockManager.lock(getAllocationLock(request, attempt), new LockCallbackNoReturn() {
            @Override
            public void doWithLockNoResult() {
                Context c = allocateTimer.time();
                try {
                    Iterator<AllocationCandidate> iter = getCandidates(attempt);
                    while (iter.hasNext()) {
                        candidatesForCPUMemoryIops.add(iter.next());
                    }
                    if (iter != null) {
                        close(iter);
                    }

                    Map<Long, Boolean> alreadyScheduledHostStatus = new HashMap<Long, Boolean>();

                    // scheduler the list and return a list that is able to schedule
                    for (AllocationCandidate candidate : candidatesForCPUMemoryIops) {
                        boolean good = true;
                        Long currentHostId = 0L;

                        // schedule cpu, memory and iops first
                        try {
                            for( Long hostId : candidate.getHosts() ){
                                currentHostId = hostId;

                                // in case we already tried to schedule instance for this hostId, then skip it
                                if (alreadyScheduledHostStatus.containsKey(hostId)) {
                                    good = alreadyScheduledHostStatus.get(hostId);
                                    continue;
                                }
                                Instance instance = attempt.getInstance();
                                good = scheduleResources("iops", attempt.getInstanceId(), false, hostId, attempt.getInstance().getAccountId());
                                if(good && InstanceConstants.KIND_VIRTUAL_MACHINE.equals(instance.getKind())) {
                                    good = scheduleResources("cpu-memory", attempt.getInstanceId(), true, hostId, attempt.getInstance().getAccountId());
                                }
                                alreadyScheduledHostStatus.put(hostId, good);
                                if (!good)
                                    break;
                            }
                        } catch (IOException e) {
                            good = false;
                            alreadyScheduledHostStatus.put(currentHostId, good);
                            log.error((e.getStackTrace()).toString(), e);
                        }
                        if (!good)
                            continue;
                        candidates.add(candidate);
                    }
                    if (candidates.size() == 0) {
                        return;
                    }
                    do {
                        Set<Constraint> failedConstraints = runAllocation(request, attempt, candidates);
                        if (attempt.getMatchedCandidate() == null) {
                            boolean removed = false;
                            // iterate over failed constraints and remove first soft constraint if any
                            Iterator<Constraint> failedIter = failedConstraints.iterator();
                            while (failedIter.hasNext() && !removed) {
                                Constraint failedConstraint = failedIter.next();
                                if (failedConstraint.isHardConstraint()) {
                                    continue;
                                }
                                attempt.getConstraints().remove(failedConstraint);
                                removed = true;
                            }
                            if (!removed) {
                                finalFailedConstraints.addAll(failedConstraints);
                                break;
                            }
                        }
                    } while (attempt.getMatchedCandidate() == null);
                } finally {
                    c.stop();
                }
            }
        });

        if (candidatesForCPUMemoryIops.size() == 0) {
            throw new FailedToAllocate("No candidates available");
        }
        if (candidates.size() == 0) {
            throw new FailedToAllocate("failed to schedule cpu/memory/iops");
        }
        if (attempt.getMatchedCandidate() == null) {
            if (finalFailedConstraints.size() > 0) {
                throw new FailedToAllocate(toErrorMessage(finalFailedConstraints));
            }
            return false;
        }

        return true;
    }

    protected String toErrorMessage(List<Constraint> constraints) {
        List<String> result = new ArrayList<>();
        for (Constraint c : constraints) {
            result.add(c.toString());
        }

        return StringUtils.join(result, ", ");
    }
    
    @Inject
    private JsonMapper jsonMapper;
    
    @Inject
    IdFormatter idFormatter;
    
    protected boolean scheduleResources(String action, Long instanceId, boolean isVM, Long hostId, Long envId) throws IOException {
        String SCHEDULE_IOPS__URL = SCHEDULER_URL + "/" + action;
        List<BasicNameValuePair> requestData = new ArrayList<>();

        requestData.add(new BasicNameValuePair("hostId", (String) idFormatter.formatId(HostConstants.TYPE, hostId)));
        requestData.add(new BasicNameValuePair(isVM ? "vmId" : "instanceId",
                (String) idFormatter.formatId(InstanceConstants.TYPE, instanceId)));
        requestData.add(new BasicNameValuePair("envId", (String) idFormatter.formatId(AccountConstants.TYPE, envId)));

        Map<String, Object> jsonData;
        HttpResponse response;
        try {
            response = Request.Post(SCHEDULE_IOPS__URL)
                    .addHeader("Accept", "application/json").bodyForm(requestData)
                    .execute().returnResponse();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.error("statusCode: {}", statusCode);
            }
            jsonData = jsonMapper.readValue(response.getEntity().getContent());

            String result = (String) jsonData.get("schedule");
            return result.equals("yes");
        } catch(HttpHostConnectException ex) {  
            log.error("Scheduler Service not reachable at [{}]", SCHEDULE_IOPS__URL);
            throw ex;
        }
    }
    
    protected Set<Constraint> runAllocation(AllocationRequest request, AllocationAttempt attempt, List<AllocationCandidate> candidates) {
        logStart(attempt);

        List<Set<Constraint>> candidateFailedConstraintSets = new ArrayList<Set<Constraint>>();
        for (AllocationCandidate candidate : candidates) {
            Set<Constraint> failedConstraints = new HashSet<Constraint>();
            attempt.getCandidates().add(candidate);

            String prefix = String.format("[%s][%s]", attempt.getId(), candidate.getId());
            logCandidate(prefix, attempt, candidate);

            boolean good = true;
            for (Constraint constraint : attempt.getConstraints()) {
                boolean match = constraint.matches(attempt, candidate);
                log.info("{}   checking candidate [{}] : {}", prefix, match, constraint);
                if (!match) {
                    good = false;
                    failedConstraints.add(constraint);
                }
            }

            log.info("{}   candidates result [{}]", prefix, good);
            if (good) {
                if (candidate.getHosts().size() > 0 && request.getType() == Type.VOLUME) {
                    throw new IllegalStateException("Attempting to allocate hosts during a volume allocation");
                }

                if (recordCandidate(attempt, candidate)) {
                    attempt.setMatchedCandidate(candidate);
                    return failedConstraints;
                } else {
                    log.info("{}   can not record result", prefix);
                }
            }
            candidateFailedConstraintSets.add(failedConstraints);
        }
        return getWeakestConstraintSet(candidateFailedConstraintSets);

    }

    // ideally we want zero hard constraints and the fewest soft constraints
    private Set<Constraint> getWeakestConstraintSet(List<Set<Constraint>> candidateFailedConstraintSets) {
        if (candidateFailedConstraintSets == null || candidateFailedConstraintSets.isEmpty()) {
            return Collections.emptySet();
        }
        Collections.sort(candidateFailedConstraintSets, new Comparator<Set<Constraint>>() {
            @Override
            public int compare(Set<Constraint> o1, Set<Constraint> o2) {
                if (o1 == o2) return 0;
                if (o1 != null && o2 == null) return 1;
                if (o1 == null && o2 != null) return -1;

                int[] o1NumOfHardAndSoftConstraints = getNumberOfConstraints(o1);
                int[] o2NumOfHardAndSoftConstraints = getNumberOfConstraints(o2);

                if (o1NumOfHardAndSoftConstraints[0] > o2NumOfHardAndSoftConstraints[0]) return 1;
                if (o1NumOfHardAndSoftConstraints[0] < o2NumOfHardAndSoftConstraints[0]) return -1;
                if (o1NumOfHardAndSoftConstraints[1] > o2NumOfHardAndSoftConstraints[1]) return 1;
                if (o1NumOfHardAndSoftConstraints[1] < o2NumOfHardAndSoftConstraints[1]) return -1;
                return 0;
            }

            private int[] getNumberOfConstraints(Set<Constraint> failedConstraints) {
                int hard = 0;
                int soft = 0;
                Iterator<Constraint> iter = failedConstraints.iterator();
                while (iter.hasNext()) {
                    Constraint c = iter.next();
                    if (c.isHardConstraint()) {
                        hard++;
                    } else {
                        soft++;
                    }
                }
                return new int[] { hard, soft };
            }

        });
        return candidateFailedConstraintSets.get(0);
    }

    protected boolean recordCandidate(AllocationAttempt attempt, AllocationCandidate candidate) {
        return allocatorDao.recordCandidate(attempt, candidate);
    }

    protected abstract LockDefinition getAllocationLock(AllocationRequest request, AllocationAttempt attempt);

    protected AllocationLog getLog(AllocationRequest request) {
        return new AllocationLog();
    }

    protected void logCandidate(String prefix, AllocationAttempt attempt, AllocationCandidate candidate) {
        log.info("{} Checking candidate:", prefix);
        for (long hostId : candidate.getHosts()) {
            log.info("{}   host [{}]", prefix, hostId);
        }
        for (Map.Entry<Long, Set<Long>> entry : candidate.getPools().entrySet()) {
            log.info("{}   volume [{}]", prefix, entry.getKey());
            for (long poolId : entry.getValue()) {
                log.info("{}     pool [{}]", prefix, poolId);
            }
        }
        for (Map.Entry<Long, Long> entry : candidate.getSubnetIds().entrySet()) {
            log.info("{}   nic [{}] subnet [{}]", prefix, entry.getKey(), entry.getValue());
        }
    }

    protected void logStart(AllocationAttempt request) {
        String id = request.getId();
        log.info("[{}] Attemping allocation for:", id);
        if (request.getInstance() != null) {
            log.info("[{}]   instance [{}]", id, request.getInstance().getId());
        }
        for (Map.Entry<Volume, Set<StoragePool>> entry : request.getPools().entrySet()) {
            long volumeId = entry.getKey().getId();
            log.info("[{}]   volume [{}]", id, volumeId);
            for (StoragePool pool : entry.getValue()) {
                log.info("[{}]     pool [{}]", id, pool.getId());
            }
        }
        log.info("[{}] constraints:", id);
        for (Constraint constraint : request.getConstraints()) {
            log.info("[{}]   {}", id, constraint);
        }
    }

    protected void close(Iterator<AllocationCandidate> iter) {
    }

    protected abstract Iterator<AllocationCandidate> getCandidates(AllocationAttempt instanceRequest);

    protected void populateConstraints(AllocationAttempt attempt, AllocationLog log) {
        List<Constraint> constraints = attempt.getConstraints();

        Instance instance = attempt.getInstance();
        Long requestedHostId = null;
        if (instance != null) {
            requestedHostId = DataAccessor.fields(instance).withKey(InstanceConstants.FIELD_REQUESTED_HOST_ID).as(Long.class);
        }

        for (AllocationConstraintsProvider provider : allocationConstraintProviders) {
            if (requestedHostId == null || provider.isCritical()) {
                provider.appendConstraints(attempt, log, constraints);
            }
        }
        Collections.sort(constraints, new Comparator<Constraint>() {
            @Override
            public int compare(Constraint o1, Constraint o2) {
                if (o1 == o2) return 0;
                if (o1 != null && o2 == null) return -1;
                if (o1 == null && o2 != null) return 1;
                if (o1.isHardConstraint() && o2.isHardConstraint()) return 0;
                if (o1.isHardConstraint() && !o2.isHardConstraint()) return -1;
                return 1;
            }
        });
    }

    protected abstract boolean supports(AllocationRequest request);

    public LockManager getLockManager() {
        return lockManager;
    }

    @Inject
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    public AllocatorDao getAllocatorDao() {
        return allocatorDao;
    }

    @Inject
    public void setAllocatorDao(AllocatorDao allocatorDao) {
        this.allocatorDao = allocatorDao;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public List<AllocationConstraintsProvider> getAllocationConstraintProviders() {
        return allocationConstraintProviders;
    }

    @Inject
    public void setAllocationConstraintProviders(List<AllocationConstraintsProvider> allocationConstraintProviders) {
        this.allocationConstraintProviders = allocationConstraintProviders;
    }

}
