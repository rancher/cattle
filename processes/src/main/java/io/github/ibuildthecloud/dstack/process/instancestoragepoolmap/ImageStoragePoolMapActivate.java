package io.github.ibuildthecloud.dstack.process.instancestoragepoolmap;

import io.github.ibuildthecloud.dstack.agent.AgentLocator;
import io.github.ibuildthecloud.dstack.agent.RemoteAgent;
import io.github.ibuildthecloud.dstack.archaius.util.ArchaiusUtil;
import io.github.ibuildthecloud.dstack.core.model.Image;
import io.github.ibuildthecloud.dstack.core.model.ImageStoragePoolMap;
import io.github.ibuildthecloud.dstack.core.model.StoragePool;
import io.github.ibuildthecloud.dstack.engine.handler.HandlerResult;
import io.github.ibuildthecloud.dstack.engine.process.ProcessInstance;
import io.github.ibuildthecloud.dstack.engine.process.ProcessState;
import io.github.ibuildthecloud.dstack.eventing.model.Event;
import io.github.ibuildthecloud.dstack.eventing.model.EventVO;
import io.github.ibuildthecloud.dstack.object.serialization.ObjectSerializer;
import io.github.ibuildthecloud.dstack.object.serialization.ObjectSerializerFactory;
import io.github.ibuildthecloud.dstack.process.base.AbstractDefaultProcessHandler;
import io.github.ibuildthecloud.dstack.util.type.InitializationTask;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import com.netflix.config.DynamicStringProperty;

//@Named
public class ImageStoragePoolMapActivate extends AbstractDefaultProcessHandler implements InitializationTask {

    AgentLocator agentLocator;
    String configPrefix = "event.data.";
    String typeName;
    String commandName = "storage.template.stage";
    Class<?> typeClass = ImageStoragePoolMap.class;
    ObjectSerializerFactory factory;
    ObjectSerializer serializer;

    @Override
    public HandlerResult handle(ProcessState state, ProcessInstance process) {
        ImageStoragePoolMap resource = (ImageStoragePoolMap)state.getResource();
        Image image = getObjectManager().loadResource(Image.class, resource.getImageId());
        StoragePool storagePool = getObjectManager().loadResource(StoragePool.class, resource.getStoragePoolId());
        RemoteAgent agent = agentLocator.lookupAgent(storagePool);

        if ( agent == null ) {
            return new HandlerResult("_noAgent", true);
        }

        ObjectSerializer serializer = getObjectSerializer();
        Map<String,Object> data = serializer.serialize(resource);
        EventVO event = EventVO.newEvent(getCommandName() == null ? process.getName() : getCommandName())
                .data(data)
                .resourceType(getObjectManager().getType(image))
                .resourceId(image.getId().toString());

        Event reply = agent.callSync(event);

        return null;
    }

    protected ObjectSerializer getObjectSerializer() {
        return serializer;
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
        String type = typeName;
        if ( type == null ) {
            type = getObjectManager().getType(typeClass);
        }

        if ( type == null ) {
            throw new IllegalStateException("Failed to find type for typeName [" + typeName + "] class [" + typeClass + "]");
        }

        serializer = getSerializer(type, false);
    }

    protected ObjectSerializer getSerializer(final String type, boolean reload) {
        DynamicStringProperty prop = ArchaiusUtil.getString(getConfigPrefix() + type);
        ObjectSerializer serialization = factory.compile(type, prop.get());

        if ( ! reload ) {
            prop.addCallback(new Runnable() {
                @Override
                public void run() {
                    getSerializer(type, true);
                }
            });
        }

        return serialization;
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

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Class<?> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<?> typeClass) {
        this.typeClass = typeClass;
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

}
