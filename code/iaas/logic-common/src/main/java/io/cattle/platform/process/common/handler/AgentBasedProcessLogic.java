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
import io.cattle.platform.object.meta.Relationship;
import io.cattle.platform.object.serialization.ObjectSerializer;
import io.cattle.platform.object.serialization.ObjectSerializerFactory;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.process.common.util.ProcessUtils;
import io.cattle.platform.process.progress.ProcessProgress;
import io.cattle.platform.process.progress.ProcessProgressInstance;
import io.cattle.platform.util.exception.ExecutionException;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.InitializationTask;
import io.cattle.platform.util.type.Priority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.config.DynamicStringProperty;

public class AgentBasedProcessLogic extends AbstractObjectProcessLogic implements InitializationTask, Priority, CompletableLogic {

    private static final String DEFAULT_NAME = "AgentBased";

    protected AgentLocator agentLocator;
    ObjectSerializerFactory factory;
    ObjectSerializer serializer;
    ProcessProgress progress;

    boolean reportProgress = false;
    boolean sendNoOp;
    String errorChainProcess;
    String configPrefix = "event.data.";
    String dataType;
    Class<?> dataTypeClass;
    String commandName;
    String[] processNames;
    String agentResourceRelationship;
    String dataResourceRelationship;
    String eventResourceRelationship;
    Integer eventRetry;

    boolean ignoreReconnecting;
    boolean shortCircuitIfAgentRemoved;
    boolean timeoutIsError;
    List<String> processDataKeys = new ArrayList<>();

    String expression;
    int priority = Priority.SPECIFIC;

    Map<String, ObjectSerializer> serializers = new ConcurrentHashMap<>();

    public AgentBasedProcessLogic() {
        if (this.getClass() == AgentBasedProcessLogic.class) {
            setName(DEFAULT_NAME);
        }
    }

    @Override
    public String[] getProcessNames() {
        if (DEFAULT_NAME.equals(getName())) {
            return new String[0];
        }
        if (processNames == null) {
            return new String[] { ProcessUtils.getDefaultProcessName(this) };
        }
        return processNames;
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
                getObjectProcessManager().scheduleProcessInstance(errorChainProcess, state.getResource(), null);
                e.setResources(state.getResource());
            }
            throw e;
        }

        return new HandlerResult(getResourceDataMap(getObjectManager().getType(context.eventResource), reply.getData()));
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
            Agent agentObj = loadResource(Agent.class, agent.getAgentId());
            if (agentObj != null && (AgentConstants.STATE_RECONNECTING.equals(agentObj.getState()) ||
                    AgentConstants.STATE_DISCONNECTED.equals(agentObj.getState()))) {
                shortCircuit = true;
            }
        }

        if (processData.size() > 0) {
            data.put("processData", processData);
        }

        EventVO<Object> event = EventVO.newEvent(getCommandName() == null ? process.getName() : getCommandName())
                .withData(data)
                .withResourceType(getObjectManager().getType(eventResource))
                .withResourceId(ObjectUtils.getId(eventResource).toString());

        preProcessEvent(event, state, process, eventResource, dataResource, agentResource);

        EventCallOptions options = new EventCallOptions();
        options.setRetry(eventRetry);
        final ProcessProgressInstance progressInstance = progress.get();

        if (reportProgress && progressInstance != null) {
            options.withProgressIsKeepAlive(true).withProgress(new EventProgress() {
                @Override
                public void progress(Event event) {
                    String message = event.getTransitioningMessage();
                    if (message != null) {
                        progressInstance.messsage(message);
                    }

                    Integer eventProgress = event.getTransitioningProgress();
                    if (eventProgress != null) {
                        progressInstance.progress(eventProgress);
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
        return getObjectByRelationship(agentResourceRelationship, state.getResource());
    }

    protected Object getEventResource(ProcessState state, ProcessInstance process) {
        return getObjectByRelationship(eventResourceRelationship, state.getResource());
    }

    protected Object getDataResource(ProcessState state, ProcessInstance process) {
        return getObjectByRelationship(dataResourceRelationship, state.getResource());
    }

    protected Object getObjectByRelationship(String relationship, Object obj) {
        if (relationship == null || obj == null) {
            return obj;
        }

        String type = getObjectManager().getType(obj);
        if (type == null) {
            throw new IllegalArgumentException("Failed to find type for [" + obj + "]");
        }

        Relationship rel = getObjectMetaDataManager().getRelationship(type, relationship);
        if (rel == null) {
            throw new IllegalStateException("Failed to find relationship [" + relationship + "] on obj [" + obj + "]");
        }

        if (rel.isListResult()) {
            throw new IllegalStateException("Relationship [" + relationship + "] on obj [" + obj + "] is a list result");
        }

        return getObjectManager().getObjectByRelationship(obj, rel);
    }

    protected String getCommandName(ProcessState state, ProcessInstance process, Object eventResource, Object dataResource, Object agentResource) {
        if (commandName != null) {
            return commandName;
        } else {
            return process.getName();
        }
    }

    protected ObjectSerializer getObjectSerializer(Object obj) {
        if (serializer != null) {
            return serializer;
        }

        String type = getObjectManager().getType(obj);
        if (type == null) {
            throw new IllegalStateException("Failed to find type for [" + obj + "]");
        }

        ObjectSerializer serializer = serializers.get(type);
        if (serializer != null) {
            return serializer;
        }

        return buildSerializer(type);
    }

    protected synchronized ObjectSerializer buildSerializer(String type) {
        ObjectSerializer serializer = serializers.get(type);
        if (serializer != null) {
            return serializer;
        }

        String expression = getExpression(type);
        if (expression == null) {
            return null;
        }

        serializer = factory.compile(type, expression);
        serializers.put(type, serializer);

        return serializer;
    }

    public String getExpression() {
        if (expression != null) {
            return expression;
        }

        if (serializer != null) {
            return serializer.getExpression();
        }

        return null;
    }

    protected String getExpression(String type) {
        if (expression != null) {
            return expression;
        }

        DynamicStringProperty prop = getExpressionProperty(type);
        prop.addCallback(new Runnable() {
            @Override
            public void run() {
                for (String type : serializers.keySet()) {
                    buildSerializer(type);
                }
            }
        });

        return prop.get();
    }

    protected DynamicStringProperty getExpressionProperty(String type) {
        return ArchaiusUtil.getString(getConfigPrefix() + type);
    }

    public AgentLocator getAgentLocator() {
        return agentLocator;
    }

    @Inject
    public void setAgentLocator(AgentLocator agentLocator) {
        this.agentLocator = agentLocator;
    }

    @Override
    public void start() {
        if (dataType == null && dataTypeClass != null) {
            dataType = getObjectManager().getType(dataTypeClass);
            if (dataType == null) {
                throw new IllegalStateException("Failed to find type for class [" + dataTypeClass + "]");
            }
        }

        if (dataType != null) {
            loadDefaultSerializer();
            getExpressionProperty(dataType).addCallback(new Runnable() {
                @Override
                public void run() {
                    loadDefaultSerializer();
                }
            });
        }
    }

    protected void loadDefaultSerializer() {
        serializer = buildSerializer(dataType);
    }

    public String getConfigPrefix() {
        return configPrefix;
    }

    public void setConfigPrefix(String configPrefix) {
        this.configPrefix = configPrefix;
    }

    public ObjectSerializerFactory getFactory() {
        return factory;
    }

    @Inject
    public void setFactory(ObjectSerializerFactory factory) {
        this.factory = factory;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public void setProcessNames(String... processNames) {
        this.processNames = processNames;
    }

    public String getAgentResourceRelationship() {
        return agentResourceRelationship;
    }

    public void setAgentResourceRelationship(String agentResourceRelationship) {
        this.agentResourceRelationship = agentResourceRelationship;
    }

    public String getDataResourceRelationship() {
        return dataResourceRelationship;
    }

    public void setDataResourceRelationship(String dataResourceRelationship) {
        this.dataResourceRelationship = dataResourceRelationship;
    }

    public String getEventResourceRelationship() {
        return eventResourceRelationship;
    }

    public void setEventResourceRelationship(String eventResourceRelationship) {
        this.eventResourceRelationship = eventResourceRelationship;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Class<?> getDataTypeClass() {
        return dataTypeClass;
    }

    public void setDataTypeClass(Class<?> dataTypeClass) {
        this.dataTypeClass = dataTypeClass;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ProcessProgress getProgress() {
        return progress;
    }

    public void setProgress(ProcessProgress progress) {
        this.progress = progress;
    }

    public boolean isReportProgress() {
        return reportProgress;
    }

    public void setReportProgress(boolean reportProgress) {
        this.reportProgress = reportProgress;
    }

    public List<String> getProcessDataKeys() {
        return processDataKeys;
    }

    public void setProcessDataKeys(List<String> processDataKeys) {
        this.processDataKeys = processDataKeys;
    }

    public boolean isShortCircuitIfAgentRemoved() {
        return shortCircuitIfAgentRemoved;
    }

    public void setShortCircuitIfAgentRemoved(boolean shortCircuitIfAgentRemoved) {
        this.shortCircuitIfAgentRemoved = shortCircuitIfAgentRemoved;
    }

    public String getErrorChainProcess() {
        return errorChainProcess;
    }

    public void setErrorChainProcess(String errorChainProcess) {
        this.errorChainProcess = errorChainProcess;
    }

    public boolean isTimeoutIsError() {
        return timeoutIsError;
    }

    public void setTimeoutIsError(boolean timeoutIsError) {
        this.timeoutIsError = timeoutIsError;
    }

    public Integer getEventRetry() {
        return eventRetry;
    }

    public void setEventRetry(Integer eventRetry) {
        this.eventRetry = eventRetry;
    }

    public boolean isSendNoOp() {
        return sendNoOp;
    }

    public void setSendNoOp(boolean sendNoOp) {
        this.sendNoOp = sendNoOp;
    }

    public boolean isIgnoreReconnecting() {
        return ignoreReconnecting;
    }

    public void setIgnoreReconnecting(boolean ignoreReconnecting) {
        this.ignoreReconnecting = ignoreReconnecting;
    }

    private static class Context {
        EventVO<?> request;
        Object eventResource;
        Object dataResource;
        Object agentResource;
    }

}