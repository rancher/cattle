package io.cattle.platform.process.instance;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.model.InstanceHostMap;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AgentBasedProcessHandler;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class LabelsProviderProcessHandler extends AgentBasedProcessHandler implements ProcessPostListener {

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Inject
    AgentInstanceFactory agentInstanceFactory;

    @Inject
    LabelsService labelsService;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Object instance = getObjectByRelationship("instance", state.getResource());
        Long accountId = (Long)ObjectUtils.getAccountId(instance);
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_LABELS_PROVIDER, accountId);
        Long agentId = agentIds.size() == 0 ? null : agentIds.get(0);
        if ((instance instanceof Instance) && agentIds.contains(((Instance) instance).getAgentId())) {
            return null;
        }
        return agentId;
    }

    @Override
    protected void postProcessEvent(EventVO<?> event, Event reply, ProcessState state, ProcessInstance process,
            Object eventResource, Object dataResource, Object agentResource) {
        Map<String, String> labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(reply.getData(),
                "instanceHostMap", "instance", "+data", "+fields", "+labels"));

        InstanceHostMap map = (InstanceHostMap)state.getResource();
        Instance instance = getObjectManager().loadResource(Instance.class, map.getInstanceId());

        for (Map.Entry<String, String>label : labels.entrySet()) {
            labelsService.createContainerLabel(instance.getAccountId(), instance.getId(), label.getKey(), label.getValue());
        }
    }
}
