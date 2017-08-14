package io.cattle.platform.process.agent;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.server.ping.PingMonitor;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.CommonStatesConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.CompletableLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.json.JsonMapper;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.util.DataAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

public class AgentActivateReconnect implements CompletableLogic {

    private static final DynamicIntProperty PING_RETRY = ArchaiusUtil.getInt("agent.activate.ping.retries");
    private static final DynamicLongProperty PING_TIMEOUT = ArchaiusUtil.getLong("agent.activate.ping.timeout");
    private static final DynamicLongProperty PING_DISCONNECT_TIMEOUT = ArchaiusUtil.getLong("agent.disconnect.after.seconds");

    private static final Logger log = LoggerFactory.getLogger(AgentActivateReconnect.class);

    ObjectManager objectManager;
    AgentLocator agentLocator;
    PingMonitor pingMonitor;
    JsonMapper jsonMapper;

    public AgentActivateReconnect(ObjectManager objectManager, AgentLocator agentLocator, PingMonitor pingMonitor, JsonMapper jsonMapper) {
        this.objectManager = objectManager;
        this.agentLocator = agentLocator;
        this.pingMonitor = pingMonitor;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        /* This will save the time */
        checkDisconnect(state);

        Agent agent = (Agent) state.getResource();
        Instance instance = objectManager.findAny(Instance.class, INSTANCE.AGENT_ID, agent.getId());

        /* Don't ping non-system container agent instances */
        if (instance != null) {
            return null;
        }

        for (String prefix : AgentConstants.AGENT_IGNORE_PREFIXES) {
            if (agent.getUri() == null || agent.getUri().startsWith(prefix)) {
                return new HandlerResult();
            }
        }

        RemoteAgent remoteAgent = agentLocator.lookupAgent(agent);
        ListenableFuture<? extends Event> future = remoteAgent.call(AgentUtils.newPing(agent)
                .withOption(Ping.STATS, true)
                .withOption(Ping.RESOURCES, true), new EventCallOptions(PING_RETRY.get(), PING_TIMEOUT.get())
                .withRetryCallback((event) -> {
                    Agent newAgent = objectManager.reload(agent);
                    if (AgentConstants.STATE_DISCONNECTING.equals(newAgent.getState()) ||
                            CommonStatesConstants.DEACTIVATING.equals(newAgent.getState())) {
                        throw new TimeoutException();
                    }
                    return event;
                }));

        return new HandlerResult().withFuture(future);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        Agent agent = (Agent)state.getResource();
        Object obj = null;
        try {
            obj = AsyncUtils.get(future);
        } catch (TimeoutException e) {
            HandlerResult result = checkDisconnect(state);
            if (result == null) {
                throw e;
            } else {
                return result;
            }
        }

        Ping resp = jsonMapper.convertValue(obj, Ping.class);
        pingMonitor.pingSuccess(agent, resp);

        return null;
    }

    protected HandlerResult checkDisconnect(ProcessState state) {
        DataAccessor acc = DataAccessor.fromMap(state.getData()).withScope(AgentActivateReconnect.class).withKey("start");
        Long startTime = acc.as(Long.class);
        if (startTime == null) {
            startTime = System.currentTimeMillis();
            acc.set(startTime);
        }

        if (PING_DISCONNECT_TIMEOUT.get() * 1000L < (System.currentTimeMillis() - startTime)) {
            return new HandlerResult().withChainProcessName(AgentConstants.PROCESS_DECONNECT);
        }

        return null;
    }

}
