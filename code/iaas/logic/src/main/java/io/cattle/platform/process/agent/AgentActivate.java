package io.cattle.platform.process.agent;

import static io.cattle.platform.core.model.tables.InstanceTable.*;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.agent.util.AgentUtils;
import io.cattle.platform.archaius.util.ArchaiusUtil;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.core.model.Instance;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.EventService;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.framework.event.Ping;
import io.cattle.platform.object.util.DataAccessor;
import io.cattle.platform.process.base.AbstractDefaultProcessHandler;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.cloudstack.managed.context.NoExceptionRunnable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;

@Named
public class AgentActivate extends AbstractDefaultProcessHandler {

    private static final DynamicIntProperty PING_RETRY = ArchaiusUtil.getInt("agent.activate.ping.retries");
    private static final DynamicLongProperty PING_TIMEOUT = ArchaiusUtil.getLong("agent.activate.ping.timeout");
    private static final DynamicLongProperty PING_DISCONNECT_TIMEOUT = ArchaiusUtil.getLong("agent.disconnect.after.seconds");

    @Inject
    AgentLocator agentLocator;
    @Inject
    EventService eventService;

    protected HandlerResult checkDisconnect(ProcessState state) {
        DataAccessor acc = DataAccessor.fromMap(state.getData()).withScope(AgentActivate.class).withKey("start");
        Long startTime = acc.as(Long.class);
        if (startTime == null) {
            startTime = System.currentTimeMillis();
            acc.set(startTime);
        }

        if (PING_DISCONNECT_TIMEOUT.get() * 1000L < (System.currentTimeMillis() - startTime)) {
            return new HandlerResult().withChainProcessName(AgentConstants.PROCESS_DECONNECT).withShouldContinue(false);
        }

        return null;
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

        boolean waitFor = DataAccessor.fromDataFieldOf(agent)
                .withScope(AgentActivate.class)
                .withKey("waitForPing")
                .withDefault(process.getName().equals(AgentConstants.PROCESS_RECONNECT))
                        .as(Boolean.class);

        RemoteAgent remoteAgent = agentLocator.lookupAgent(agent);
        final ListenableFuture<? extends Event> future = remoteAgent.call(AgentUtils.newPing(agent)
                .withOption(Ping.STATS, true)
                .withOption(Ping.RESOURCES, true), new EventCallOptions(PING_RETRY.get(), PING_TIMEOUT.get()));
        future.addListener(new NoExceptionRunnable() {
            @Override
            protected void doRun() {
                try {
                    Event resp = future.get();
                    EventVO<?> respCopy = new EventVO<>(resp);
                    respCopy.setName("ping.reply");
                    eventService.publish(respCopy);
                } catch (Exception e) {
                }
            }
        }, MoreExecutors.sameThreadExecutor());


        if (waitFor) {
            try {
                AsyncUtils.get(future);
            } catch (TimeoutException e) {
                HandlerResult result = checkDisconnect(state);
                if (result == null) {
                    throw e;
                } else {
                    return result;
                }
            }
        }
        HandlerResult result = new HandlerResult();
        if (process.getName().equalsIgnoreCase(AgentConstants.PROCESS_RECONNECT)) {
            result.shouldDelegate(true);
        }
        return result;
    }

    @Override
    public String[] getProcessNames() {
        return new String[] { AgentConstants.PROCESS_ACTIVATE, AgentConstants.PROCESS_RECONNECT };
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

}
