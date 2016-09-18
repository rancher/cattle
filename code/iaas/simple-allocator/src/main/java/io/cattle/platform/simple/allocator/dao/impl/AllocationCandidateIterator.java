package io.cattle.platform.simple.allocator.dao.impl;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Port;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.simple.allocator.AllocationCandidateCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllocationCandidateIterator implements Iterator<AllocationCandidate> {

    private static final Logger log = LoggerFactory.getLogger(AllocationCandidateIterator.class);

    List<Long> volumeIds;
    Iterator<Map.Entry<Long, Set<Long>>> iterator;
    Map<Long, String> hostIdsToUuids;
    Map<Long, List<Port>> hostIdsToUsedPorts;
    Stack<AllocationCandidate> candidates = new Stack<AllocationCandidate>();
    ObjectManager objectManager;
    boolean hosts;
    AllocationCandidateCallback callback;

    public AllocationCandidateIterator(ObjectManager objectManager, LinkedHashMap<Long, Set<Long>> hostsAndStoragePools, Map<Long, String> hostIdsToUuids,
            Map<Long, List<Port>> hostIdsToUsedPorts, List<Long> volumeIds, boolean hosts, AllocationCandidateCallback callback) {
        super();
        this.objectManager = objectManager;
        this.volumeIds = volumeIds;
        this.iterator = hostsAndStoragePools.entrySet().iterator();
        this.hostIdsToUuids = hostIdsToUuids;
        this.hostIdsToUsedPorts = hostIdsToUsedPorts;
        this.hosts = hosts;
        this.callback = callback;
    }

    @Override
    public boolean hasNext() {
        if (candidates.size() > 0) {
            return true;
        }

        return readNext();
    }

    protected boolean readNext() {
        if (iterator.hasNext()) {
            Map.Entry<Long, Set<Long>>hostAndPools = iterator.next();
            enumerate(hostAndPools.getKey(), hostAndPools.getValue());
        }

        return candidates.size() > 0;
    }

    protected void enumerate(Long hostId, Set<Long> pools) {
        log.debug("Enumerating canditates hostId [{}] pools {}", hostId, pools);

        if (hostId == null) {
            return;
        }

        Long candidateHostId = this.hosts ? hostId : null;
        String candidateHostUuid = this.hosts ? this.hostIdsToUuids.get(hostId) : null;
        List<Port> usedPorts =  hostIdsToUsedPorts != null ? hostIdsToUsedPorts.get(hostId) : new ArrayList<Port>();

        Map<Pair<Class<?>, Long>, Object> cache = new HashMap<Pair<Class<?>, Long>, Object>();

        if (volumeIds.size() == 0) {
            pushCandidate(new AllocationCandidate(objectManager, cache, candidateHostId, candidateHostUuid, usedPorts, Collections.<Long, Long> emptyMap()));
        }

        for (List<Pair<Long, Long>> pairs : traverse(volumeIds, pools)) {
            Map<Long, Long> volumeToPool = new HashMap<Long, Long>();
            for (Pair<Long, Long> pair : pairs) {
                volumeToPool.put(pair.getLeft(), pair.getRight());
            }

            pushCandidate(new AllocationCandidate(objectManager, cache, candidateHostId, candidateHostUuid, usedPorts, volumeToPool));
        }
    }

    protected void pushCandidate(AllocationCandidate candidate) {
        if (callback == null) {
            candidates.push(candidate);
        } else {
            for (AllocationCandidate c : callback.withCandidate(candidate)) {
                candidates.push(c);
            }
        }
    }

    public static <L, R> List<List<Pair<L, R>>> traverse(List<L> lefts, Set<R> rights) {
        Stack<Pair<L, R>> pairSet = new Stack<Pair<L, R>>();
        List<List<Pair<L, R>>> pairSets = new ArrayList<List<Pair<L, R>>>();

        traverse(lefts, rights, 0, pairSet, pairSets);

        return pairSets;
    }

    // Writing this code made me feel very stupid. There has got be a simpler
    // way of doing this.
    public static <L, R> void traverse(List<L> lefts, Set<R> rights, int i, Stack<Pair<L, R>> pairSet, List<List<Pair<L, R>>> pairSets) {
        if (i == lefts.size()) {
            pairSets.add(new ArrayList<Pair<L, R>>(pairSet));
            return;
        }

        L left = lefts.get(i);
        for (R right : rights) {
            pairSet.push(new ImmutablePair<L, R>(left, right));
            traverse(lefts, rights, i + 1, pairSet, pairSets);
            pairSet.pop();
        }
    }

    @Override
    public AllocationCandidate next() {
        return candidates.pop();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
