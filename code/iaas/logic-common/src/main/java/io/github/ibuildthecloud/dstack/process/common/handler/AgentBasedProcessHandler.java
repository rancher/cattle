package io.github.ibuildthecloud.dstack.process.common.handler;

import io.github.ibuildthecloud.dstack.agent.AgentLocator;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.object.meta.Relationship;
import io.github.ibuildthecloud.dstack.object.serialization.ObjectSerializer;
import io.github.ibuildthecloud.dstack.object.serialization.ObjectSerializerFactory;
import io.github.ibuildthecloud.dstack.object.util.ObjectUtils;
import io.github.ibuildthecloud.dstack.process.common.util.ProcessUtils;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;
import io.github.ibuildthecloud.dstack.util.type.Priority;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.netflix.config.DynamicStringProperty;

public class AgentBasedProcessHandler extends AbstractObjectProcessHandler implements InitializationTask, Priority {

    private static final String DEFAULT_NAME = "AgentBased";

    AgentLocator agentLocator;
    ObjectSerializerFactory factory;
    ObjectSerializer serializer;

    String configPrefix = "event.data.";
    String dataType;
    Class<?> dataTypeClass;
    String commandName;
    String[] processNames;
    String agentResourceRelationship;
    String dataResourceRelationship;
    String eventResourceRelationship;
    String expression;
    int priority = Priority.SPECIFIC;

    Map<String,ObjectSerializer> serializers = new ConcurrentHashMap<String, ObjectSerializer>();

    public AgentBasedProcessHandler() {
        if ( this.getClass() == AgentBasedProcessHandler.class ) {
            setName(DEFAULT_NAME);
        }
    }

    @Override
    public String[] getProcessNames() {
        if ( DEFAULT_NAME.equals(getName()) ) {
            return new String[0];
        }
        if ( processNames == null ) {
            return new String[] { ProcessUtils.getDefaultProcessName(this) };
        }
        return processNames;
    }

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        Object eventResource = getEventResource(state, process);
        Object dataResource = getDataResource(state, process);
        Object agentResource = getAgentResource(state, process, dataResource);

        if ( eventResource == null ) {
            throw new IllegalStateException("Event resource is null");
        }

        if ( dataResource == null ) {
            throw new IllegalStateException("Data resource is null");
        }

        RemoteAgent agent = agentLocator.lookupAgent(agentResource);

        if ( agent == null ) {
            return new HandlerResult("_noAgent", true);
        }

        ObjectSerializer serializer = getObjectSerializer(dataResource);
        Map<String,Object> data = serializer.serialize(dataResource);
        EventVO event = EventVO.newEvent(getCommandName() == null ? process.getName() : getCommandName())
                .withData(data)
                .withResourceType(getObjectManager().getType(eventResource))
                .withResourceId(ObjectUtils.getId(eventResource).toString());

        preProcessEvent(event, state, process, eventResource, dataResource, agentResource);

        Event reply = agent.callSync(event);

        postProcessEvent(event, reply, state, process, eventResource, dataResource, agentResource);

        return null;
    }

    protected void postProcessEvent(EventVO event, Event reply, ProcessState state, ProcessInstance process,
            Object eventResource, Object dataResource, Object agentResource) {
    }

    protected void preProcessEvent(EventVO event, ProcessState state, ProcessInstance process, Object eventResource,
            Object dataResource, Object agentResource) {
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
        if ( relationship == null || obj == null ) {
            return obj;
        }

        String type = getObjectManager().getType(obj);
        if ( type == null ) {
            throw new IllegalArgumentException("Failed to find type for [" + obj + "]");
        }

        Relationship rel = getObjectMetaDataManager().getRelationship(type, relationship);
        if ( rel == null ) {
            throw new IllegalStateException("Failed to find relationship [" + relationship + "] on obj [" + obj + "]");
        }

        if ( rel.isListResult() ) {
            throw new IllegalStateException("Relationship [" + relationship + "] on obj [" + obj + "] is a list result");
        }

        return getObjectManager().getObjectByRelationship(obj, rel);
    }

    protected String getCommandName(ProcessState state, ProcessInstance process,Object eventResource,
            Object dataResource, Object agentResource) {
        if ( commandName != null ) {
            return commandName;
        } else {
            return process.getName();
        }
    }

    protected ObjectSerializer getObjectSerializer(Object obj) {
        if ( serializer != null ) {
            return serializer;
        }

        String type = getObjectManager().getType(obj);
        if ( type == null ) {
            throw new IllegalStateException("Failed to find type for [" + obj + "]");
        }

        ObjectSerializer serializer = serializers.get(type);
        if ( serializer != null ) {
            return serializer;
        }

        return buildSerializer(type);
    }

    protected synchronized ObjectSerializer buildSerializer(String type) {
        ObjectSerializer serializer = serializers.get(type);
        if ( serializer != null ) {
            return serializer;
        }

        serializer = factory.compile(type, getExpression(type));
        serializers.put(type, serializer);

        return serializer;
    }

    public String getExpression() {
        if ( expression != null ) {
            return expression;
        }

        if ( serializer != null ) {
            return serializer.getExpression();
        }

        return null;
    }

    protected String getExpression(String type) {
        if ( expression != null ) {
            return expression;
        }

        DynamicStringProperty prop = getExpressionProperty(type);
        if ( StringUtils.isBlank(prop.get()) ) {
            throw new IllegalStateException("Failed to find expression for object serialization for type [" + type +
                    "] config property [" + getConfigPrefix() + type + "] is blank");
        }

        prop.addCallback(new Runnable() {
            @Override
            public void run() {
                for ( String type : serializers.keySet() ) {
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
        if ( dataType == null && dataTypeClass != null) {
            dataType = getObjectManager().getType(dataTypeClass);
            if ( dataType == null ) {
                throw new IllegalStateException("Failed to find type for class [" + dataTypeClass + "]");
            }
        }

        if ( dataType != null ) {
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

    @Override
    public void stop() {
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

    public void setProcessNames(String[] processNames) {
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

}