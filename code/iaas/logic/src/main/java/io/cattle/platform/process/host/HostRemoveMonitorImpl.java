package io.cattle.platform.process.host;

import io.cattle.platform.core.dao.AgentDao;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.core.dao.HostDao;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.engine.process.util.ProcessEngineUtils;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.process.StandardProcess;
import io.cattle.platform.task.Task;

import javax.inject.Inject;

public class HostRemoveMonitorImpl implements Task {

    @Inject
    HostDao hostDao;
    @Inject
    AgentDao agentDao;
    @Inject
    ObjectProcessManager processManager;
    @Inject
    GenericResourceDao resourceDao;

    @Override
    public void run() {
        if (!ProcessEngineUtils.enabled()) {
            return;
        }

        hostDao.updateNullUpdatedHosts();

        for (Host host : hostDao.findHostsRemove()) {
            processManager.scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE, StandardProcess.REMOVE,
                    host, null);
        }

        for (Agent agent : agentDao.findAgentsToRemove()) {
            processManager.scheduleStandardChainedProcessAsync(StandardProcess.DEACTIVATE, StandardProcess.REMOVE,
                    agent, null);
        }
    }

    @Override
    public String getName() {
        return "evacuate.hosts";
    }

}
