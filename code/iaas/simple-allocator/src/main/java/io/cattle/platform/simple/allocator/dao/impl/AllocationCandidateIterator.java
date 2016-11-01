package io.cattle.platform.simple.allocator.dao.impl;

import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
    Iterator<CandidateHostInfo> iterator;
    Stack<AllocationCandidate> candidates = new Stack<AllocationCandidate>();
    ObjectManager objectManager;
    boolean hosts;

    public AllocationCandidateIterator(ObjectManager objectManager, List<CandidateHostInfo> hostInfos, List<Long> volumeIds, boolean hosts) {
        super();
        this.objectManager = objectManager;
        this.iterator = hostInfos.iterator();
        this.volumeIds = volumeIds;
        this.hosts = hosts;
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
            CandidateHostInfo hostInfo = iterator.next();
            enumerate(hostInfo);
        }

        return candidates.size() > 0;
    }

    protected void enumerate(CandidateHostInfo hostInfo) {
        log.debug("Enumerating canditates hostId [{}] pools {}", hostInfo.getHostId(), hostInfo.getPoolIds());

        Long candidateHostId = this.hosts ? hostInfo.getHostId() : null;
        Map<Pair<Class<?>, Long>, Object> cache = new HashMap<Pair<Class<?>, Long>, Object>();

        if (volumeIds.size() == 0) {
            pushCandidate(new AllocationCandidate(objectManager, cache, candidateHostId, hostInfo.getHostUuid(), hostInfo.getUsedPorts(),
                    Collections.<Long, Long> emptyMap()));
        }

        for (List<Pair<Long, Long>> pairs : traverse(volumeIds, hostInfo.getPoolIds())) {
            Map<Long, Long> volumeToPool = new HashMap<Long, Long>();
            for (Pair<Long, Long> pair : pairs) {
                volumeToPool.put(pair.getLeft(), pair.getRight());
            }

            pushCandidate(new AllocationCandidate(objectManager, cache, candidateHostId, hostInfo.getHostUuid(), hostInfo.getUsedPorts(), volumeToPool));
        }
    }

    protected void pushCandidate(AllocationCandidate candidate) {
        candidates.push(candidate);
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
