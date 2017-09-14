package io.cattle.platform.loop;

import io.cattle.platform.core.addon.HealthcheckState;
import io.cattle.platform.core.addon.metadata.HostInfo;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.constants.HealthcheckConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.model.Loop;
import io.cattle.platform.metadata.Metadata;
import io.cattle.platform.metadata.MetadataManager;
import io.cattle.platform.object.ObjectManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class HealthcheckScheduleLoop implements Loop {

    long accountId;
    MetadataManager metadataManager;
    ObjectManager objectManager;

    public HealthcheckScheduleLoop(long accountId, MetadataManager metadataManager, ObjectManager objectManager) {
        this.accountId = accountId;
        this.metadataManager = metadataManager;
        this.objectManager = objectManager;
    }

    @Override
    public Result run(List<Object> input) {
        Metadata metadata = metadataManager.getMetadataForAccount(accountId);

        Set<Long> hostIds = validHostIds(metadata);
        if (hostIds.size() == 0) {
            return Result.DONE;
        }

        if (hostIds.size() == 1) {
            // Special case, every healthcheck should be on this one host.
            return handleSingleHost(metadata, hostIds.iterator().next());
        }

        long[] hosts = hostIds.stream().mapToLong((x) -> x).toArray();
        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            if (instanceInfo.getHealthCheck() == null || instanceInfo.getHostId() == null) {
                continue;
            }

            Set<Long> toAvoid = new HashSet<>();
            toAvoid.add(instanceInfo.getHostId());

            List<HealthcheckState> finalHcs = new ArrayList<>();

            for (HealthcheckState healthcheckState : instanceInfo.getHealthCheckHosts()) {
                if (toAvoid.contains(healthcheckState.getHostId())) {
                    continue;
                }
                if (!hostIds.contains(healthcheckState.getHostId())) {
                    continue;
                }
                toAvoid.add(healthcheckState.getHostId());
                finalHcs.add(healthcheckState);
            }

            boolean added = false;
            for (int i = finalHcs.size() ; i < 3 ; i++) {
                Long nextHostId = nextHost(hosts, toAvoid);
                if (nextHostId == null) {
                    break;
                }
                added = true;
                toAvoid.add(nextHostId);
                finalHcs.add(new HealthcheckState(nextHostId, HealthcheckConstants.HEALTH_STATE_INITIALIZING));
            }

            if (added) {
                writeHcs(metadata, instanceInfo.getId(), finalHcs);
            }
        }

        return Result.DONE;
    }

    protected Long nextHost(long[] hosts, Set<Long> toAvoid) {
        int start = ThreadLocalRandom.current().nextInt(hosts.length);
        int i = start;
        do {
            long nextHostId = hosts[i];
            if (!toAvoid.contains(nextHostId)) {
                return nextHostId;
            }
            i = (i + 1) % hosts.length;
        } while(i != start);

        return null;
    }

    protected Result handleSingleHost(Metadata metadata, Long hostId) {
        List<HealthcheckState> desired = Arrays.asList(new HealthcheckState(hostId, HealthcheckConstants.HEALTH_STATE_INITIALIZING));

        for (InstanceInfo instanceInfo : metadata.getInstances()) {
            if (instanceInfo.getHealthCheck() == null) {
                continue;
            }
            List<HealthcheckState> hcs = instanceInfo.getHealthCheckHosts();
            if (hcs.size() != 1 || !hostId.equals(hcs.get(0).getHostId())) {
                writeHcs(metadata, instanceInfo.getId(), desired);
            }
        }

        return Result.DONE;
    }

    protected void writeHcs(Metadata metadata, long instanceId, List<HealthcheckState> hcs) {
        metadata.modify(Instance.class, instanceId, (instance) -> {
            return objectManager.setFields(instance,
                    InstanceConstants.FIELD_HEALTHCHECK_STATES, hcs);
        });
    }

    protected Set<Long> validHostIds(Metadata metadata) {
        Set<Long> hosts = new HashSet<>();

        for (HostInfo hostInfo : metadata.getHosts()) {
            if (CommonStatesConstants.ACTIVE.equals(hostInfo.getAgentState())) {
                hosts.add(hostInfo.getId());
            }
        }

        return hosts;
    }

}
