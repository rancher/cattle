package io.cattle.platform.process.common.handler;

import io.cattle.platform.agent.AgentLocator;
import io.cattle.platform.agent.RemoteAgent;
import io.cattle.platform.archaius.util.ArchaiusUtil;
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
import io.cattle.platform.eventing.EventProgress;
import io.cattle.platform.eventing.exception.AgentRemovedException;
import io.cattle.platform.eventing.exception.EventExecutionException;
import io.cattle.platform.eventing.model.Event;
import io.cattle.platform.eventing.model.EventVO;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.process.progress.ProcessProgressInstance;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Named;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicStringProperty;

public class AgentBasedProcessHandler implements CompletableLogic, Named {

    DynamicStringProperty expressionProp;
    protected AgentLocator agentLocator;
    protected ObjectSerializerFactory factory;
    protected ObjectSerializer serializer;
    protected ObjectManager objectManager;
    protected ObjectProcessManager processManager;
    protected ProcessProgress progress;

    protected boolean sendNoOp;
    protected String name;
    protected String errorChainProcess;
    protected String configPrefix = "event.data.";
    protected String dataType;
    protected Class<?> dataTypeClass;
    protected String commandName;
    protected Integer eventRetry;

    protected boolean ignoreReconnecting;
    protected boolean shortCircuitIfAgentRemoved;
    protected boolean timeoutIsError;
    protected List<String> processDataKeys = new ArrayList<>();

    public AgentBasedProcessHandler(AgentLocator agentLocator, ObjectSerializerFactory factory, ObjectManager objectManager,
            ObjectProcessManager processManager) {
        super();
        this.agentLocator = agentLocator;
        this.factory = factory;
        this.objectManager = objectManager;
        this.processManager = processManager;
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
            return new HandlerResult(true, CollectionUtils.asMap((Object) "_noAgent", true));
        }

        ListenableFuture<?> future = handleEvent(state, process, eventResource, dataResource, agentResource, agent);
        if (EngineContext.isNestedExecution() || future.isDone()) {
            return complete(future, state, process);
        }

        return new HandlerResult().withFuture(future);
    }

    @Override
    public HandlerResult complete(ListenableFuture<?> future, ProcessState state, ProcessInstance process) {
        Context context = (Context) state.getData().get(getName()+".context");
        Event reply = null;
        try {
            reply = (Event) AsyncUtils.get(future);
            postProcessEvent(context.request, reply, state, process,
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

        return new HandlerResult(getResourceDataMap(objectManager.getType(context.eventResource), reply.getData()));
    }

    protected ListenableFuture<?> handleEvent(ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource,
            RemoteAgent agent) {
        ObjectSerializer serializer = getObjectSerializer(dataResource);
        Map<String, Object> data = serializer == null ? null : serializer.serialize(dataResource);

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
            options.withProgressIsKeepAlive(true).withProgress(new EventProgress() {
                @Override
                public void progress(Event event) {
                    String message = event.getTransitioningMessage();
                    if (message != null) {
                        progressInstance.messsage(message);
                    }
                }
            });
        }

        if (shortCircuit) {
            return AsyncUtils.done(EventVO.reply(event));
        } else {
            return agent.call(event, options);
        }
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

    protected synchronized ObjectSerializer getObjectSerializer(Object obj) {
        if (serializer != null) {
            return serializer;
        }

        String type = objectManager.getType(obj);
        if (type == null) {
            throw new IllegalStateException("Failed to find type for [" + obj + "]");
        }

        return this.serializer = buildSerializer(type);
    }

    protected ObjectSerializer buildSerializer(String type) {
        String expression = getExpression(type);
        if (expression == null) {
            return null;
        }

        return factory.compile(type, expression);
    }

    protected String getExpression(String type) {
        if (expressionProp == null) {
            expressionProp = ArchaiusUtil.getString(configPrefix + type);
        }
        return expressionProp.get();
    }

    protected DynamicStringProperty getExpressionProperty(String type) {
        return ArchaiusUtil.getString(configPrefix + type);
    }

    protected void loadDefaultSerializer() {
        if (dataType == null && dataTypeClass == null) {
            throw new IllegalStateException("dataType or dataTypeClass");
        }

        if (dataType == null) {
            dataType = objectManager.getType(dataTypeClass);
        }

        serializer = buildSerializer(dataType);
    }

    private static class Context {
        EventVO<?> request;
        Object eventResource;
        Object dataResource;
        Object agentResource;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}