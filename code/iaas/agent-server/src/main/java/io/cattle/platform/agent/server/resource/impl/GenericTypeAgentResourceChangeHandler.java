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
import java.util.HashSet;
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
    Set<String> keys = null;
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
            Object value = ObjectUtils.getPropertyIgnoreErrors(resource, key);
            try {
                Field field = translateField(null, key, value);
                if ( field != null ) {
                    data.put(field.getName(), field.getValue());
                }
            } catch (MissingDependencyException e) {
            }
        }

        return data;
    }

    protected Object loadResource(Map<String, Object> resource) {
        Object uuid = resource.get(ObjectMetaDataManager.UUID_FIELD);
        return objectManager.findOne(getTypeClass(), ObjectMetaDataManager.UUID_FIELD, uuid);
    }

    protected Map<String, Object> reload(Map<String, Object> resource) {
        Object uuid = resource.get(ObjectMetaDataManager.UUID_FIELD);
        if ( uuid == null ) {
            throw new IllegalStateException("Can not reload a resource with out a UUID");
        }

        return load(uuid.toString());
    }

    protected synchronized Set<String> getKeys() {
        if ( keys == null ) {
            keys = new HashSet<String>(FIELDS);
            keys.addAll(getAdditionalKeys());
        }

        return keys;
    }

    protected Set<String> getAdditionalKeys() {
        return new HashSet<String>();
    }

    protected Set<String> getChangableKeys() {
        return new HashSet<String>();
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

        resource = checkFields(resource);

        resourceDao.createAndSchedule(getTypeClass(), resource);
    }

    protected Map<String,Object> checkFields(Map<String,Object> resource) throws MissingDependencyException {
        Map<String,Object> result = new HashMap<String, Object>();
        for ( Map.Entry<String, Object> entry : resource.entrySet() ) {
            Field field = translateField(resource, entry.getKey(), entry.getValue());
            if ( field != null ) {
                result.put(field.getName(), field.getValue());
            }
        }

        return result;
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
    public void changed(Map<String, Object> agentResource, Map<String, Object> loadedResource) throws MissingDependencyException {
        Map<String,Object> changes = new HashMap<String, Object>();

        for ( String changableKey : getChangableKeys() ) {
            Object agentObj = agentResource.get(changableKey);
            Object loadedObj = loadedResource.get(changableKey);

            if ( ! org.apache.commons.lang3.ObjectUtils.equals(agentObj, loadedObj) ) {
                Field field = translateField(loadedResource, changableKey, agentObj);
                if ( field != null ) {
                    changes.put(field.getName(), field.getValue());
                }
            }
        }

        if ( changes.size() > 0 ) {
            Object resource = loadResource(loadedResource);
            objectManager.setFields(resource, changes);
            loadedResource = reload(loadedResource);
        }

        if ( areDifferent(agentResource, loadedResource) ) {
            throw new UnsupportedOperationException("Agent resource change from "  + loadedResource + " to " +
                    agentResource);
        }
    }

    protected Field translateField(Map<String, Object> resource, String field, Object value) throws MissingDependencyException {
        return new Field(field, value);
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
