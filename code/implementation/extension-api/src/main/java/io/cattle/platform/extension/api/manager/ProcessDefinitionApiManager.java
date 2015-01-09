package io.cattle.platform.extension.api.manager;

import io.cattle.platform.engine.process.ExtensionBasedProcessDefinition;
import io.cattle.platform.engine.process.ProcessDefinition;
import io.cattle.platform.extension.api.dot.DotMaker;
import io.cattle.platform.extension.api.model.ProcessDefinitionApi;
import io.cattle.platform.extension.api.util.ApiPredicates;
import io.github.ibuildthecloud.gdapi.factory.SchemaFactory;
import io.github.ibuildthecloud.gdapi.model.ListOptions;
import io.github.ibuildthecloud.gdapi.model.Resource;
import io.github.ibuildthecloud.gdapi.request.ApiRequest;
import io.github.ibuildthecloud.gdapi.request.resource.impl.AbstractNoOpResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.google.common.base.Predicate;

public class ProcessDefinitionApiManager extends AbstractNoOpResourceManager {

    public static final String PROCESS_DOT = "processDot";

    private static final Map<String, String> LINKS = new HashMap<String, String>();
    static {
        LINKS.put(PROCESS_DOT, null);
    }

    List<ProcessDefinition> processDefinitions;
    DotMaker dotMaker;

    public ProcessDefinitionApiManager() {
        addResourceToCreateResponse(ProcessDefinitionApi.class);
    }

    @Override
    public Class<?>[] getTypeClasses() {
        return new Class<?>[] { ProcessDefinitionApi.class };
    }

    @Override
    protected Map<String, String> getLinks(SchemaFactory schemaFactory, Resource resource) {
        return LINKS;
    }

    @Override
    protected Object getLinkInternal(String type, String id, String link, ApiRequest request) {
        ProcessDefinition def = getById(id);
        if (def == null) {
            return null;
        }

        String dot = null;
        if (PROCESS_DOT.equalsIgnoreCase(link)) {
            dot = dotMaker.getProcessDot(def);
        }

        boolean written = false;
        try {
            written = dotMaker.writeResponse(dot, request);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return written ? new Object() : null;
    }

    @Override
    protected Object getByIdInternal(String type, String id, ListOptions options) {
        return toApi(getById(id));
    }

    protected ProcessDefinition getById(String id) {
        for (ProcessDefinition def : processDefinitions) {
            if (def.getName().equals(id)) {
                return def;
            }
        }

        return null;
    }

    @Override
    protected Object listInternal(SchemaFactory schemaFactory, String type, Map<Object, Object> criteria, ListOptions options) {
        List<ProcessDefinitionApi> result = new ArrayList<ProcessDefinitionApi>();
        Predicate<Object> condition = ApiPredicates.filterOn(criteria, "name", "resourceType");

        for (ProcessDefinition def : processDefinitions) {
            if (condition.apply(def)) {
                result.add(toApi(def));
            }
        }

        return result;
    }

    protected ProcessDefinitionApi toApi(ProcessDefinition def) {
        if (def == null) {
            return null;
        }

        ProcessDefinitionApi api = new ProcessDefinitionApi();

        api.setName(def.getName());
        api.setResourceType(def.getResourceType());
        api.setStateTransitions(def.getStateTransitions());

        if (def instanceof ExtensionBasedProcessDefinition) {
            api.setExtensionBased(true);
            api.setPreProcessListeners(((ExtensionBasedProcessDefinition) def).getPreProcessListenersExtensionPoint());
            api.setProcessHandlers(((ExtensionBasedProcessDefinition) def).getProcessHandlersExtensionPoint());
            api.setPostProcessListeners(((ExtensionBasedProcessDefinition) def).getPostProcessListenersExtensionPoint());
        }

        return api;
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
