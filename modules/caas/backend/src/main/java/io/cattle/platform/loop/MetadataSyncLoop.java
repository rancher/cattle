package io.cattle.platform.loop;

import io.cattle.platform.core.addon.Removed;
import io.cattle.platform.core.addon.metadata.InstanceInfo;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.manager.LoopFactory;
import io.cattle.platform.engine.manager.LoopManager;
import io.cattle.platform.engine.model.Loop;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MetadataSyncLoop implements Loop {

    long accountId;
    LoopManager loopManager;
    Set<Long> agentIds = new HashSet<>();

    public MetadataSyncLoop(long accountId, LoopManager loopManager) {
        this.accountId = accountId;
        this.loopManager = loopManager;
    }

    @Override
    public Result run(List<Object> input) {
        for (Object obj : input) {
            if (obj instanceof Removed) {
                agentIds.remove(getTargetAgentId(((Removed) obj).getRemoved()));
            } else {
                Long agentId = getTargetAgentId(obj);
                if (agentId != null) {
                    agentIds.add(agentId);
                }
            }
        }

        for (Long agentId : agentIds) {
            loopManager.kick(LoopFactory.METADATA_CLIENT, Agent.class, agentId, input);
        }

        return Result.DONE;
    }

    protected Long getTargetAgentId(Object obj) {
        if (!(obj instanceof InstanceInfo)) {
            return null;
        }

        InstanceInfo instance = (InstanceInfo) obj;
        if (instance.getAccountId() != accountId) {
            return null;
        }

        if (instance.isNativeContainer()) {
            return null;
        }

        Long agentId = instance.getAgentId();
        if (agentId == null) {
            return null;
        }

        if ("true".equals(instance.getLabels().get(SystemLabels.LABEL_AGENT_SERVICE_METADATA))) {
            return agentId;
        }

        return null;
    }

}
