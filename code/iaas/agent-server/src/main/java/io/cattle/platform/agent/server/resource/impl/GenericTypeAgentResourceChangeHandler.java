package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.agent.server.resource.AgentResourceChangeHandler;
import io.cattle.platform.core.constants.AgentConstants;
import io.cattle.platform.core.dao.GenericResourceDao;
import io.cattle.platform.object.ObjectManager;
import io.cattle.platform.object.meta.ObjectMetaDataManager;
import io.cattle.platform.object.process.ObjectProcessManager;
import io.cattle.platform.object.util.ObjectUtils;
import io.cattle.platform.util.type.CollectionUtils;
import io.cattle.platform.util.type.Priority;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class GenericTypeAgentResourceChangeHandler implements AgentResourceChangeHandler, Priority {

    private static final Set<String> FIELDS = CollectionUtils.set(
            ObjectMetaDataManager.UUID_FIELD,
            ObjectMetaDataManager.KIND_FIELD);

    int priority = Priority.SPECIFIC;
    String type;
    Class<?> typeClass;
    SchemaFactory schemaFactory;
    ObjectManager objectManager;
    ObjectProcessManager processManager;
    GenericResourceDao resourceDao;

    @Override
    public Map<String, Object> load(String uuid) {
        Object resource = objectManager.findOne(getTypeClass(), ObjectMetaDataManager.UUID_FIELD, uuid);
        if ( resource == null ) {
            return null;
        }

        Map<String,Object> data = new HashMap<String, Object>();
        for ( String key : getKeys() ) {
            data.put(key, ObjectUtils.getPropertyIgnoreErrors(resource, key));
        }

        return data;
    }

    protected Set<String> getKeys() {
        return FIELDS;
    }

    protected Class<?> getTypeClass() {
        if ( typeClass == null ) {
            typeClass = schemaFactory.getSchemaClass(type);
        }

        return typeClass;
    }

    @Override
    public void newResource(long agentId, Map<String, Object> resource) throws MissingDependencyException {
        resource.put(AgentConstants.ID_REF, agentId);
        resourceDao.createAndSchedule(getTypeClass(), resource);
    }

    @Override
    public boolean areDifferent(Map<String, Object> agentResource, Map<String, Object> loadedResource) {
        Map<String,Object> filtered = new HashMap<String, Object>();
        for ( String key : getKeys() ) {
            filtered.put(key, agentResource.get(key));
        }

        return ! filtered.equals(loadedResource);
    }

    @Override
    public void changed(Map<String, Object> agentResource, Map<String, Object> loadedResource) {
        throw new UnsupportedOperationException("Agent resource change from "  + loadedResource + " to " +
                agentResource);
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public SchemaFactory getSchemaFactory() {
        return schemaFactory;
    }

    @Inject
    public void setSchemaFactory(SchemaFactory schemaFactory) {
        this.schemaFactory = schemaFactory;
    }

    public ObjectManager getObjectManager() {
        return objectManager;
    }

    @Inject
    public void setObjectManager(ObjectManager objectManager) {
        this.objectManager = objectManager;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public ObjectProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ObjectProcessManager processManager) {
        this.processManager = processManager;
    }

    public GenericResourceDao getResourceDao() {
        return resourceDao;
    }

    @Inject
    public void setResourceDao(GenericResourceDao resourceDao) {
        this.resourceDao = resourceDao;
    }

}
