package io.cattle.platform.process.instance;

import io.cattle.iaas.labels.service.LabelsService;
import io.cattle.platform.agent.instance.dao.AgentInstanceDao;
import io.cattle.platform.agent.instance.factory.AgentInstanceFactory;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.core.util.SystemLabels;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.process.common.handler.AgentBasedProcessLogic;
import io.cattle.platform.util.type.CollectionUtils;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public abstract class LabelsProviderProcessHandler extends AgentBasedProcessLogic {

    @Inject
    AgentInstanceDao agentInstanceDao;

    @Inject
    AgentInstanceFactory agentInstanceFactory;

    @Inject
    LabelsService labelsService;

    @Override
    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        Instance instance = getInstance(state);
        Long accountId = instance.getAccountId();
        List<Long> agentIds = agentInstanceDao.getAgentProvider(SystemLabels.LABEL_AGENT_SERVICE_LABELS_PROVIDER, accountId);
        Long agentId = agentIds.size() == 0 ? null : agentIds.get(0);
        if ((instance instanceof Instance) && agentIds.contains(instance.getAgentId())) {
            return null;
        }
        return agentId;
    }

    protected Instance getInstance(ProcessState state) {
        Object instance = state.getResource();
        if (!(instance instanceof Instance)) {
            instance = getObjectByRelationship("instance", instance);
        }
        return (Instance)instance;
    }

    @Override
    protected void preProcessEvent(EventVO<?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource,
            Object agentResource) {
        Map<String, Object> data = CollectionUtils.toMap(event.getData());
        if (!data.containsKey("instance")) {
            Object instanceData = CollectionUtils.getNestedValue(data, "instanceHostMap", "instance");
            data.put("instance", instanceData);
        }
        super.preProcessEvent(event, state, process, eventResource, dataResource, agentResource);
    }

    @Override
    protected void postProcessEvent(EventVO<?> event, Event reply, ProcessState state, ProcessInstance process,
            Object eventResource, Object dataResource, Object agentResource) {
        Map<String, String> labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(reply.getData(),
                "instance", "+data", "+fields", "+labels"));
        if (labels.size() == 0) {
            labels = CollectionUtils.toMap(CollectionUtils.getNestedValue(reply.getData(),
                    "instanceHostMap", "instance", "+data", "+fields", "+labels"));
        } else {
            CollectionUtils.setNestedValue(CollectionUtils.toMap(reply.getData()), labels,
                    "instanceHostMap", "instance", "+data", "+fields", "+labels");
        }

        Instance instance = getInstance(state);

        for (Map.Entry<String, String>label : labels.entrySet()) {
            labelsService.createContainerLabel(instance.getAccountId(), instance.getId(), label.getKey(), label.getValue());
        }
    }
}
