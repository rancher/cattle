package io.cattle.platform.extension.api.manager;

import io.cattle.platform.api.utils.ApiUtils;
import io.cattle.platform.engine.manager.ProcessManager;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.extension.api.dot.DotMaker;
import io.cattle.platform.extension.api.model.ProcessDefinitionApi;
import io.cattle.platform.extension.api.model.ResourceDefinition;
import io.cattle.platform.util.type.CollectionUtils;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.id.IdFormatter;
import io.github.ibuildthecloud.gdapi.model.Include;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.model.Schema;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.ResourceManager;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class ResourceDefinitionManager extends AbstractNoOpResourceManager {

    public static final String PROCESSES = "processes";
    public static final String RESOURCE_DOT = "resourceDot";

    private static final Map<String, String> LINKS = new HashMap<String, String>();
    static {
        LINKS.put(PROCESSES, null);
        LINKS.put(RESOURCE_DOT, null);
    }

    List<ProcessDefinition> processDefinitions;
    ProcessManager processManager;
    DotMaker dotMaker;

    public ResourceDefinitionManager() {
        addResourceToCreateResponse(ResourceDefinition.class);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ResourceDefinition.class };
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        for (ProcessDefinition def : processDefinitions) {
            if (id.equalsIgnoreCase(def.getResourceType())) {
                return newResource(def.getResourceType(), options.getInclude());
            }
        }

        return null;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        Set<String> found = new HashSet<String>();
        List<ResourceDefinition> result = new ArrayList<ResourceDefinition>();
        for (ProcessDefinition def : processDefinitions) {
            String resourceType = def.getResourceType();

            if (found.contains(resourceType)) {
                continue;
            }

            result.add(newResource(resourceType, options.getInclude()));
            found.add(resourceType);
        }

        return result;
    }

    protected ResourceDefinition newResource(String name, Include include) {
        ResourceDefinition def = new ResourceDefinition(name);
        if (include != null) {
            for (String link : include.getLinks()) {
                for (Object obj : getLinkInternal(def, link, null)) {
                    ApiUtils.addAttachement(def, link, obj);
                }
            }
        }

        return def;
    }

    @Override
    protected Resource constructResource(final IdFormatter idFormatter, SchemaFactory schemaFactory, final Schema schema, Object obj, ApiRequest apiRequest) {
        return ApiUtils.createResourceWithAttachments(this, apiRequest, idFormatter, schemaFactory, schema, obj, new HashMap<String, Object>());
    }

    protected List<?> getLinkInternal(ResourceDefinition def, String link, ApiRequest request) {
        String type = request.getSchemaFactory().getSchemaName(ProcessDefinitionApi.class);
        ResourceManager rm = getLocator().getResourceManagerByType(type);

        if (link.equalsIgnoreCase(PROCESSES)) {
            Map<Object, Object> criteria = CollectionUtils.asMap((Object) "resourceType", def.getId());
            return CollectionUtils.toList(rm.list(type, criteria, new ListOptions()));
        } else if (request != null && link.equalsIgnoreCase(RESOURCE_DOT)) {
            String dot = dotMaker.getResourceDot(def.getName());

            boolean written = false;
            try {
                written = dotMaker.writeResponse(dot, request);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            return written ? Collections.emptyList() : null;
        }

        return null;
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        ResourceDefinition def = (ResourceDefinition) getById(type, id, new ListOptions());
        return def == null ? null : getLinkInternal(def, link, request);
    }

    @Override
    protected Map<String, String> getLinks(SchemaFactory schemaFactory, Resource resource) {
        return LINKS;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    @Inject
    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    public DotMaker getDotMaker() {
        return dotMaker;
    }

    @Inject
    public void setDotMaker(DotMaker dotMaker) {
        this.dotMaker = dotMaker;
    }

    public List<ProcessDefinition> getProcessDefinitions() {
        return processDefinitions;
    }

    @Inject
    public void setProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        this.processDefinitions = processDefinitions;
    }

}
