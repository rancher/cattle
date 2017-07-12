package io.cattle.platform.process.common.handler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.async.utils.AsyncUtils;
import io.cattle.platform.async.utils.TimeoutException;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.constants.InstanceConstants;
import io.cattle.platform.core.model.Agent;
import io.cattle.platform.engine.context.EngineContext;
import io.cattle.platform.engine.handler.CompletableLogic;
import io.cattle.platform.engine.handler.HandlerResult;
import io.cattle.platform.engine.process.ProcessInstance;
import io.cattle.platform.engine.process.ProcessState;
import io.cattle.platform.eventing.EventCallOptions;
import io.cattle.platform.eventing.exception.AgentRemovedException;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.process.progress.ProcessProgressInstance;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Named;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AgentBasedProcessHandler implements CompletableLogic, Named {

    protected AgentLocator agentLocator;
    protected ObjectSerializer serializer;
    protected ObjectManager objectManager;
    protected ObjectProcessManager processManager;
    protected ProcessProgress progress;

    protected boolean sendNoOp;
    protected String name;
    protected String errorChainProcess;
    protected String commandName;
    protected Integer eventRetry;

    protected boolean ignoreReconnecting;
    protected boolean shortCircuitIfAgentRemoved;
    protected boolean timeoutIsError;
    protected List<String> processDataKeys = new ArrayList<>();

    public AgentBasedProcessHandler(AgentLocator agentLocator, ObjectSerializer serializer, ObjectManager objectManager,
            ObjectProcessManager processManager) {
        this.agentLocator = agentLocator;
        this.serializer = serializer;
        this.objectManager = objectManager;
        this.processManager = processManager;
        this.name = getClass().getSimpleName();
    }

    @Override
    public final HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object eventResource = getEventResource(state, process);
        Object dataResource = getDataResource(state, process);
        Object agentResource = getAgentResource(state, process, dataResource);

        if (eventResource == null) {
            return null;
        }

        if (dataResource == null) {
            return null;
        }

        RemoteAgent agent = agentLocator.lookupAgent(agentResource);
        if (agent == null) {
            return new HandlerResult(true, CollectionUtils.asMap("_noAgent", true));
        }

        ListenableFuture<?> future = handleEvent(state, process, eventResource, dataResource, agentResource, agent);
        if (EngineContext.isNestedExecution() || future.isDone()) {
            return complete(future, state, process);
        }

        return new HandlerResult().withFuture(future);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        Context context;
        try {
            context = (Context)AsyncUtils.get(future);
            postProcessEvent(context.request, context.reply, state, process,
                context.eventResource, context.dataResource, context.agentResource);
        } catch (AgentRemovedException e) {
            if (shortCircuitIfAgentRemoved) {
                return null;
            } else {
                throw e;
            }
        } catch (TimeoutException e) {
            if (timeoutIsError) {
                throw new ExecutionException(e);
            } else {
                throw e;
            }
        } catch (EventExecutionException e) {
            if (StringUtils.isNotBlank(errorChainProcess)) {
                processManager.scheduleProcessInstance(errorChainProcess, state.getResource(), null);
                e.setResources(state.getResource());
            }
            throw e;
        }

        return new HandlerResult(getResourceDataMap(objectManager.getType(context.eventResource), context.reply.getData()));
    }

    protected ListenableFuture<?> handleEvent(ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource,
            RemoteAgent agent) {
        Map<String, Object> data = serializer.serialize(dataResource);

        boolean shortCircuit = false;
        Map<String, Object> processData = new HashMap<>();
        for (String key : processDataKeys) {
            Object value = state.getData().get(key);
            if (value != null) {
                shortCircuit = InstanceConstants.PROCESS_DATA_NO_OP.equals(key) && Boolean.TRUE.equals(value);
                processData.put(key, value);
            }
        }

        if (sendNoOp) {
            shortCircuit = false;
        }

        if (ignoreReconnecting && !shortCircuit) {
            Agent agentObj = objectManager.loadResource(Agent.class, agent.getAgentId());
            if (agentObj != null && (AgentConstants.STATE_RECONNECTING.equals(agentObj.getState()) ||
                    AgentConstants.STATE_DISCONNECTED.equals(agentObj.getState()))) {
                shortCircuit = true;
            }
        }

        if (processData.size() > 0) {
            data.put("processData", processData);
        }

        EventVO<Object> event = EventVO.newEvent(getCommandName(process))
                .withData(data)
                .withResourceType(objectManager.getType(eventResource))
                .withResourceId(ObjectUtils.getId(eventResource).toString());

        preProcessEvent(event, state, process, eventResource, dataResource, agentResource);

        EventCallOptions options = new EventCallOptions();
        options.setRetry(eventRetry);

        if (progress != null) {
            final ProcessProgressInstance progressInstance = progress.get();
            progressInstance.init(state);
            options.withProgressIsKeepAlive(true).withProgress(event1 -> {
                String message = event1.getTransitioningMessage();
                if (message != null) {
                    progressInstance.messsage(message);
                }
            });
        }

        ListenableFuture<?> result;
        if (shortCircuit) {
            result = AsyncUtils.done(EventVO.reply(event));
        } else {
            result = agent.call(event, options);
        }

        return Futures.transform(result, (f) -> {
            return new Context(event, (Event)f, eventResource, dataResource, agentResource);
        });
    }

    protected Map<Object, Object> getResourceDataMap(String type, Object data) {
        Object result = CollectionUtils.toMap(data).get(type);
        return result == null ? null : CollectionUtils.toMap(result);
    }

    protected void postProcessEvent(EventVO<?> event, Event reply, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource,
            Object agentResource) {
    }

    protected void preProcessEvent(EventVO<?> event, ProcessState state, ProcessInstance process, Object eventResource, Object dataResource,
            Object agentResource) {
    }

    protected Object getAgentResource(ProcessState state, ProcessInstance process, Object dataResource) {
        return state.getResource();
    }

    protected Object getEventResource(ProcessState state, ProcessInstance process) {
        return state.getResource();
    }

    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        return state.getResource();
    }

    protected String getCommandName(ProcessInstance process) {
        if (commandName != null) {
            return commandName;
        } else {
            return process.getName();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static class Context {
        EventVO<?> request;
        Event reply;
        Object eventResource;
        Object dataResource;
        Object agentResource;

        public Context(EventVO<?> request, Event reply, Object eventResource, Object dataResource, Object agentResource) {
            this.reply = reply;
            this.request = request;
            this.eventResource = eventResource;
            this.dataResource = dataResource;
            this.agentResource = agentResource;
        }
    }

}