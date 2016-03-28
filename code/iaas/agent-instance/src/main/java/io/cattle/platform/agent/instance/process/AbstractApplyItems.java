package io.cattle.platform.agent.instance.process;

import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.configitem.request.ConfigUpdateRequest;
import io.cattle.platform.configitem.request.util.ConfigUpdateRequestUtils;
import io.cattle.platform.configitem.version.ConfigItemStatusManager;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.NetworkService;
import io.cattle.platform.core.model.NetworkServiceProvider;
import io.cattle.platform.engine.handler.ProcessPostListener;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.handler.AbstractObjectProcessLogic;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;
import org.slf4j.Logger;

import com.netflix.config.DynamicStringListProperty;

public abstract class AbstractApplyItems extends AbstractObjectProcessLogic implements ProcessPostListener {

    private static final DynamicStringListProperty BASE = ArchaiusUtil.getList("agent.instance.services.base.items");
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractApplyItems.class);

    @Inject
    @Named("CoreExecutorService")
    ExecutorService executorService;
    JsonMapper jsonMapper;
    ConfigItemStatusManager statusManager;
    boolean assignBase = true;

    protected void assignItems(NetworkServiceProvider provider, Agent agent, Object owner, ProcessState state, ProcessInstance processInstance,
                               boolean waitFor) {
        if (agent == null) {
            return;
        }

        String contextId = getContext(processInstance, owner, state.getResource());

        ConfigUpdateRequest request = ConfigUpdateRequestUtils.getRequest(jsonMapper, state, contextId);
        if (request == null) {
            log.trace("ITEMS: New request agent [{}]", agent.getId());
            request = ConfigUpdateRequest.forResource(Agent.class, agent.getId());
            if (waitFor) {
                ConfigUpdateRequestUtils.setWaitFor(request);
            }
            if (assignBase) {
                log.trace("ITEMS: assign base items [{}]", agent.getId());
                assignBaseItems(provider, request, agent, processInstance);
                log.trace("ITEMS: done assign base items [{}]", agent.getId());
            }
            log.trace("ITEMS: assign items [{}]", agent.getId());
            assignServiceItems(provider, request, agent, state, processInstance);
            log.trace("ITEMS: done assign items [{}]", agent.getId());
        } else {
            log.trace("ITEMS: update config [{}]", agent.getId());
            statusManager.updateConfig(request);
            log.trace("ITEMS: done update config [{}]", agent.getId());
        }

        if (request != null) {
            ConfigUpdateRequestUtils.setRequest(request, state, contextId);
        }
    }

    protected abstract String getConfigPrefix();

    protected void assignServiceItems(final NetworkServiceProvider provider, final ConfigUpdateRequest request, final Agent agent, ProcessState state,
            ProcessInstance processInstance) {
        final Set<String> apply = new HashSet<String>();
        final Set<String> increment = new HashSet<String>();
        String prefix = String.format("%s%s.%s", getConfigPrefix(), processInstance.getName(), provider.getKind());

        for (NetworkService service : objectManager.children(provider, NetworkService.class)) {
            apply.addAll(ArchaiusUtil.getList(String.format("%s.%s.apply", prefix, service.getKind())).get());
            increment.addAll(ArchaiusUtil.getList(String.format("%s.%s.increment", prefix, service.getKind())).get());
        }

        setItems(request, apply, increment);

        log.trace("ITEMS: update config [{}]", agent.getId());
        statusManager.updateConfig(request);
        log.trace("ITEMS: done update config [{}]", agent.getId());

        executorService.submit(new NoExceptionRunnable() {
            @Override
            protected void doRun() throws Exception {
                for (Agent otherAgent : getOtherAgents(provider, request, agent)) {
                    if (otherAgent.getId().equals(agent.getId())) {
                        continue;
                    }

                    log.trace("ITEMS: other set items [{}]", otherAgent.getId());
                    ConfigUpdateRequest otherRequest = ConfigUpdateRequest.forResource(Agent.class, otherAgent.getId());
                    setItems(otherRequest, apply, increment);
                    log.trace("ITEMS: done other set items [{}]", otherAgent.getId());

                    log.trace("ITEMS: other update config [{}]", otherAgent.getId());
                    statusManager.updateConfig(otherRequest);
                    log.trace("ITEMS: done other update config [{}]", otherAgent.getId());
                }
            }
        });
    }

    protected abstract List<? extends Agent> getOtherAgents(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent);

    protected void setItems(ConfigUpdateRequest request, Set<String> apply, Set<String> increment) {
        for (String item : apply) {
            request.addItem(item).withApply(true).withIncrement(false).withCheckInSyncOnly(true);
        }

        for (String item : increment) {
            request.addItem(item).withApply(true).withIncrement(true).withCheckInSyncOnly(false);
        }
    }

    public String getContext(ProcessInstance instance, Object obj, Object resource) {
        return String.format("%s:%s:%s:%s:%s", instance.getName(),
                objectManager.getType(obj), ObjectUtils.getId(obj),
                objectManager.getType(resource), ObjectUtils.getId(resource));
    }

    protected void assignBaseItems(NetworkServiceProvider provider, ConfigUpdateRequest request, Agent agent, ProcessInstance processInstance) {
        String key = String.format("%s%s.%s.base.items", getConfigPrefix(), processInstance.getName(), provider.getKind());

        if (ArchaiusUtil.getBoolean(key).get()) {
            for (String item : BASE.get()) {
                request.addItem(item).withApply(true).withIncrement(false).withCheckInSyncOnly(true);
            }
        }
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    @Inject
    public void setJsonMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public ConfigItemStatusManager getStatusManager() {
        return statusManager;
    }

    @Inject
    public void setStatusManager(ConfigItemStatusManager statusManager) {
        this.statusManager = statusManager;
    }

    public boolean isAssignBase() {
        return assignBase;
    }

    public void setAssignBase(boolean assignBase) {
        this.assignBase = assignBase;
    }

}
