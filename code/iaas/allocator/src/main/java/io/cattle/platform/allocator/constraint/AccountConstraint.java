package io.cattle.platform.allocator.constraint;

import io.cattle.platform.allocator.service.AllocationAttempt;
import io.cattle.platform.allocator.service.AllocationCandidate;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.StoragePool;
import io.cattle.platform.core.model.Subnet;
import io.cattle.platform.object.ObjectManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccountConstraint extends HardConstraint implements Constraint {

    /* Caches */
    Map<Long, Host> hosts = new HashMap<>();
    Map<Long, StoragePool> pools = new HashMap<>();
    Map<Long, Subnet> subnets = new HashMap<>();

    ObjectManager objectManager;
    long accountId;

    public AccountConstraint(ObjectManager objectManager, long accountId) {
        this.objectManager = objectManager;
        this.accountId = accountId;
    }

    @Override
    public boolean matches(AllocationAttempt attempt, AllocationCandidate candidate) {
        if (!checkHosts(attempt, candidate)) {
            return false;
        }

        if (!checkPools(attempt, candidate)) {
            return false;
        }

        /* TODO: Disable subnet checking because we still have one shared network */
//        if (!checkSubnets(attempt, candidate)) {
//            return false;
//        }

        return true;
    }

    protected boolean checkHosts(AllocationAttempt attempt, AllocationCandidate candidate) {
        for (long hostId : candidate.getHosts()) {
            Host host = hosts.get(hostId);
            if (host == null) {
                host = objectManager.loadResource(Host.class, hostId);
                hosts.put(hostId, host);
            }

            if (host == null) {
                return false;
            }

            if (host.getAccountId().longValue() != accountId) {
                return false;
            }
        }

        return true;
    }

    protected boolean checkPools(AllocationAttempt attempt, AllocationCandidate candidate) {
        for (Set<Long> ids : candidate.getPools().values()) {
            for (long poolId : ids) {
                StoragePool pool = pools.get(poolId);
                if (pool == null) {
                    pool = objectManager.loadResource(StoragePool.class, poolId);
                    pools.put(poolId, pool);
                }

                if (pool == null) {
                    return false;
                }

                if (pool.getAccountId().longValue() != accountId) {
                    return false;
                }
            }
        }

        return true;
    }

    protected boolean checkSubnets(AllocationAttempt attempt, AllocationCandidate candidate) {
        for (long subnetId : candidate.getSubnetIds().values()) {
            Subnet subnet = subnets.get(subnetId);
            if (subnet == null) {
                subnet = objectManager.loadResource(Subnet.class, subnetId);
                subnets.put(subnetId, subnet);
            }

            if (subnet == null) {
                return false;
            }

            if (subnet.getAccountId().longValue() != accountId) {
                return false;
            }
        }

        return true;
    }

    public long getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return String.format("account id must be %d", accountId);
    }

}
