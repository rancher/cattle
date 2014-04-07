package io.cattle.platform.simple.allocator.network;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Network;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.simple.allocator.AllocationCandidateCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Sets;

public class NetworkAllocationCandidates implements AllocationCandidateCallback {

    AllocationAttempt attempt;
    Set<List<Pair<Long,Long>>> subnetIds = new HashSet<List<Pair<Long,Long>>>();
    ObjectManager objectManager;

    @SuppressWarnings("unchecked")
    public NetworkAllocationCandidates(ObjectManager objectManager, AllocationAttempt attempt) {
        this.attempt = attempt;

        List<Set<Pair<Long,Long>>> nicToSubnets = new ArrayList<Set<Pair<Long,Long>>>();

        for ( Nic nic : attempt.getNics() ) {
            Set<Pair<Long,Long>> nicToSubnet = new HashSet<Pair<Long,Long>>();

            Subnet existingSubnet = attempt.getSubnets().get(nic);

            if ( existingSubnet == null ) {
                Network network = objectManager.loadResource(Network.class, nic.getNetworkId());
                if ( network != null ) {
                    for ( Subnet subnet : objectManager.children(network, Subnet.class) ) {
                        nicToSubnet.add(new ImmutablePair<Long, Long>(nic.getId(), subnet.getId()));
                    }
                }
            } else {
                nicToSubnet.add(new ImmutablePair<Long, Long>(nic.getId(), existingSubnet.getId()));
            }

            if ( nicToSubnet.size() > 0 ) {
                nicToSubnets.add(nicToSubnet);
            }
        }

        if ( nicToSubnets.size() == 1 ) {
            subnetIds.add(new ArrayList<Pair<Long,Long>>(nicToSubnets.get(0)));
        } else if ( nicToSubnets.size() > 1 ) {
            subnetIds = Sets.cartesianProduct(nicToSubnets.toArray(new Set[nicToSubnets.size()]));
        }
    }

    @Override
    public List<AllocationCandidate> withCandidate(AllocationCandidate candidate) {
        if ( subnetIds.size() == 0 ) {
            return Arrays.asList(candidate);
        }

        List<AllocationCandidate> result = new ArrayList<AllocationCandidate>();

        for ( List<Pair<Long,Long>> nicsToSubnetIds : subnetIds ) {
            AllocationCandidate withNics = new AllocationCandidate(candidate);
            for ( Pair<Long,Long> nicsToSubnetId : nicsToSubnetIds ) {
                withNics.getSubnetIds().put(nicsToSubnetId.getLeft(), nicsToSubnetId.getRight());
            }
            result.add(withNics);
        }

        return result;
    }

}
