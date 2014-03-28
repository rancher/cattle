package io.cattle.platform.agent.server.resource.impl;

import io.cattle.platform.agent.server.resource.dao.MappedResourceChangeHandlerDao;
import io.cattle.platform.core.constants.AgentConstants;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class MappedTypeAgentResourceChangeHandler extends GenericTypeAgentResourceChangeHandler {

    private static final String MAPPED_UUID = "mappedUuids";

    MappedResourceChangeHandlerDao mappedTypeDao;
    Class<?> resourceType;
    Class<?> mappingType;
    Class<?> otherType;
    String resourceUuidKey;
    Set<String> keys;

    @Override
    public Map<String, Object> load(String uuid) {
        Map<String, Object> resource = super.load(uuid);
        if ( resource == null ) {
            return null;
        }

        Set<String> uuids = mappedTypeDao.getMappedUuids(uuid, resourceType, mappingType, otherType);
        resource.put(MAPPED_UUID, uuids);

        return resource;
    }

    @Override
    protected synchronized Set<String> getKeys() {
        if ( keys == null ) {
            keys = new HashSet<String>(super.getKeys());
            keys.add(MAPPED_UUID);
        }

        return keys;
    }

    @Override
    public void newResource(long agentId, Map<String, Object> resource) throws MissingDependencyException {
        Object mappedUuid = resource.get(resourceUuidKey);

        if ( mappedUuid == null ) {
            throw new IllegalStateException("Missing " + resourceUuidKey + " on resource [" + resource + "] from agent [" + agentId + "]");
        }

        resource.put(AgentConstants.ID_REF, agentId);

        mappedTypeDao.newResource(resourceType, mappingType, otherType, resource, mappedUuid.toString());
    }

    @Override
    public boolean areDifferent(Map<String, Object> agentResource, Map<String, Object> loadedResource) {
        Object otherUuid = agentResource.get(resourceUuidKey);
        @SuppressWarnings("unchecked")
        Set<String> otherUuids = new HashSet<String>((Set<String>)loadedResource.get(MAPPED_UUID));
        if ( otherUuid != null ) {
            otherUuids.add(otherUuid.toString());
        }
        agentResource.put(MAPPED_UUID, otherUuids);

        return super.areDifferent(agentResource, loadedResource);
    }

    public MappedResourceChangeHandlerDao getMappedTypeDao() {
        return mappedTypeDao;
    }

    @Inject
    public void setMappedTypeDao(MappedResourceChangeHandlerDao mappedTypeDao) {
        this.mappedTypeDao = mappedTypeDao;
    }

    public Class<?> getResourceType() {
        return resourceType;
    }

    @Inject
    public void setResourceType(Class<?> resourceType) {
        this.resourceType = resourceType;
    }

    public Class<?> getMappingType() {
        return mappingType;
    }

    @Inject
    public void setMappingType(Class<?> mappingType) {
        this.mappingType = mappingType;
    }

    public Class<?> getOtherType() {
        return otherType;
    }

    @Inject
    public void setOtherType(Class<?> otherType) {
        this.otherType = otherType;
    }

    public String getResourceUuidKey() {
        return resourceUuidKey;
    }

    @Inject
    public void setResourceUuidKey(String resourceUuidKey) {
        this.resourceUuidKey = resourceUuidKey;
    }

    public void setKeys(Set<String> keys) {
        this.keys = keys;
    }

}
