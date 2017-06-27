package io.cattle.platform.extension.api.manager;

import io.cattle.platform.api.resource.AbstractNoOpResourceManager;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.extension.api.model.ResourceDefinition;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResourceDefinitionManager extends AbstractNoOpResourceManager {

    Collection<ProcessDefinition> processDefinitions;

    public ResourceDefinitionManager(Collection<ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

    @Override
    public Object getById(String type, String id, ListOptions options) {
        for (ProcessDefinition def : processDefinitions) {
            if (id.equalsIgnoreCase(def.getResourceType())) {
                return newResource(def.getResourceType());
            }
        }

        return null;
    }

    @Override
    public Object list(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Set<String> found = new HashSet<>();
        List<ResourceDefinition> result = new ArrayList<>();
        for (ProcessDefinition def : processDefinitions) {
            String resourceType = def.getResourceType();

            if (found.contains(resourceType)) {
                continue;
            }

            result.add(newResource(resourceType));
            found.add(resourceType);
        }

        return result;
    }

    protected ResourceDefinition newResource(String name) {
        return new ResourceDefinition(name);
    }

}
