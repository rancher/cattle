package io.cattle.platform.agent.instance.ipsec.process;

import io.cattle.platform.core.constants.NetworkServiceConstants;
import io.cattle.platform.core.dao.NetworkDao;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.resource.pool.PooledResourceOptions;
import io.cattle.platform.resource.pool.ResourcePoolManager;
import io.cattle.platform.resource.pool.util.ResourcePoolConstants;

import java.util.List;

import javax.inject.Inject;

public class AgentInstanceIpsecNicDeactivate extends AbstractObjectProcessLogic implements ProcessPostListener {

    ResourcePoolManager poolManager;
    NetworkDao networkDao;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.deactivate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();
        Instance instance = loadResource(Instance.class, nic.getInstanceId());

        if ( nic.getDeviceNumber() == null || nic.getDeviceNumber() != 0 ||
                instance == null || instance.getAgentId() == null ) {
            return null;
        }

        List<? extends NetworkService> service = networkDao.getAgentInstanceNetworkService(instance.getId(),
                NetworkServiceConstants.KIND_IPSEC_TUNNEL);

        if ( service.size() == 0 ) {
            return null;
        }

        for ( Host host : mappedChildren(instance, Host.class) ) {
            poolManager.releaseResource(host, instance, new PooledResourceOptions()
                    .withQualifier(ResourcePoolConstants.HOST_PORT));
        }

        return null;
    }

    public ResourcePoolManager getPoolManager() {
        return poolManager;
    }

    @Inject
    public void setPoolManager(ResourcePoolManager poolManager) {
        this.poolManager = poolManager;
    }

    public NetworkDao getNetworkDao() {
        return networkDao;
    }

    @Inject
    public void setNetworkDao(NetworkDao networkDao) {
        this.networkDao = networkDao;
    }

}
