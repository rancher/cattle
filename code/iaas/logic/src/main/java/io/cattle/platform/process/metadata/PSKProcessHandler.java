package io.cattle.platform.process.metadata;

import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.configitem.request.ConfigUpdateItem;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PSKProcessHandler extends AbstractObjectProcessLogic implements ProcessPreListener {

    @Inject
    AgentInstanceDao agentInstanceDao;
    @Inject
    ConfigItemStatusManager statusManager;

    @Override
    public String[] getProcessNames() {
        return new String[] {InstanceConstants.PROCESS_START};
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Instance instance = (Instance)state.getResource();
        if (!Boolean.TRUE.equals(instance.getSystem())) {
            return null;
        }

        List<Long> agentIds = agentInstanceDao.getAgentProviderIgnoreHealth(SystemLabels.LABEL_AGENT_SERVICE_IPSEC, instance.getAccountId());
        for (long agentId : agentIds) {
            ConfigUpdateRequest request = ConfigUpdateRequest.forResource(Agent.class, agentId);
            ConfigUpdateItem item = request.addItem("psk");
            item.setApply(true);
            item.setIncrement(false);
            statusManager.updateConfig(request);
        }

        return null;
    }

}
