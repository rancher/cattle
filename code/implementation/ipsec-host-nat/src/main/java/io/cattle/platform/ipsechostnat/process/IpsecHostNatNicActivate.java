package io.cattle.platform.ipsechostnat.process;

import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.Nic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.iaas.network.NetworkServiceManager;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpsecHostNatNicActivate extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final Logger log = LoggerFactory.getLogger(IpsecHostNatNicActivate.class);

    public static final String SERVICE = "ipsecHostNatService";

    NetworkServiceManager serviceManager;
    AgentInstanceFactory agentInstanceFactory;

    @Override
    public String[] getProcessNames() {
        return new String[] { "nic.activate" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Nic nic = (Nic)state.getResource();
        NetworkService service = serviceManager.getService(nic.getNetworkId(), SERVICE);

        if ( service == null ) {
            return null;
        }

        Instance instance = objectManager.loadResource(Instance.class, nic.getInstanceId());

        if ( instance == null ) {
            return null;
        }

        if ( nic.getVnetId() != null ) {
            Instance agentInstance = agentInstanceFactory
                                    .newBuilder()
                                    .withNetworkService(service)
                                    .withInstance(instance)
                                    .forVnetId(nic.getVnetId())
                                    .build();

            log.info("Agent instance [{}] for nic [{}] instance [{}]",
                    agentInstance.getId(), nic.getId(), nic.getInstanceId());
        }

        return null;
    }

    public NetworkServiceManager getServiceManager() {
        return serviceManager;
    }

    @Inject
    public void setServiceManager(NetworkServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public AgentInstanceFactory getAgentInstanceFactory() {
        return agentInstanceFactory;
    }

    @Inject
    public void setAgentInstanceFactory(AgentInstanceFactory agentInstanceFactory) {
        this.agentInstanceFactory = agentInstanceFactory;
    }

}
