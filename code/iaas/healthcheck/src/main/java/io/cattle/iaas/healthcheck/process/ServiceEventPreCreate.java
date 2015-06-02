package io.cattle.iaas.healthcheck.process;

import static io.cattle.platform.core.model.tables.AgentTable.*;
import static io.cattle.platform.core.model.tables.InstanceTable.*;
import static io.cattle.platform.core.model.tables.ServiceEventTable.*;

import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.HealthcheckInstance;
import io.cattle.platform.core.model.HealthcheckInstanceHostMap;
import io.cattle.platform.core.model.Host;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.ServiceEvent;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.handler.ProcessPreListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;
import io.cattle.platform.util.type.Priority;

import java.util.List;

public class ServiceEventPreCreate extends AbstractObjectProcessLogic implements ProcessPreListener, Priority {

    @Override
    public String[] getProcessNames() {
        return new String[] { "serviceevent.create" };
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ServiceEvent event = (ServiceEvent)state.getResource();
        Agent agent = objectManager.findOne(Agent.class, AGENT.ACCOUNT_ID, event.getAccountId());

        if (agent == null || event.getExternalTimestamp() == null) {
            return null;
        }

        Instance instance = null;

        for (Instance check : objectManager.find(Instance.class, INSTANCE.AGENT_ID, agent.getId())) {
            if (check.getRemoved() == null) {
                instance = check;
                break;
            }
        }

        if (instance == null) {
            return null;
        }

        List<Host> hosts = objectManager.mappedChildren(instance, Host.class);
        if (hosts.size() == 0) {
            return null;
        }

        agent = objectManager.loadResource(Agent.class, hosts.get(0).getAgentId());

        if (agent == null) {
            return null;
        }

        HealthcheckInstanceHostMap healthcheckInstanceHostMap = objectManager.findOne(HealthcheckInstanceHostMap.class,
                ObjectMetaDataManager.UUID_FIELD, event.getHealthcheckUuid());

        if (healthcheckInstanceHostMap == null) {
            return null;
        }

        HealthcheckInstance healthcheckInstance = objectManager.loadResource(HealthcheckInstance.class,
                healthcheckInstanceHostMap.getHealthcheckInstanceId());

        if (healthcheckInstance == null) {
            return null;
        }

        Long resourceAccId = DataAccessor.fromDataFieldOf(agent)
                .withKey(AgentConstants.DATA_AGENT_RESOURCES_ACCOUNT_ID)
                .as(Long.class);

        if (healthcheckInstanceHostMap.getAccountId().equals(resourceAccId)) {
            return new HandlerResult(
                ObjectMetaDataManager.ACCOUNT_FIELD, healthcheckInstanceHostMap.getAccountId(),
                SERVICE_EVENT.INSTANCE_ID, healthcheckInstance.getInstanceId(),
                SERVICE_EVENT.HEALTHCHECK_INSTANCE_ID, healthcheckInstance.getId(),
                SERVICE_EVENT.HOST_ID, healthcheckInstanceHostMap.getHostId()
            );
        }

        return null;
    }

    @Override
    public int getPriority() {
        return Priority.DEFAULT;
    }
}
